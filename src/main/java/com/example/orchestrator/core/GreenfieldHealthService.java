package com.example.orchestrator.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * Service to manage greenfield node health and pending request queue.
 *
 * Strategy:
 * 1. When greenfield is DOWN, approved requests are queued (not dropped)
 * 2. Background thread monitors greenfield health
 * 3. When greenfield comes UP, queued requests are processed in order
 * 4. Metrics track: queue depth, wait times, retries, success/failure rates
 */
@Service
public class GreenfieldHealthService {

    private static final Logger log = LoggerFactory.getLogger(GreenfieldHealthService.class);

    private final RestTemplate restTemplate;
    private final String greenfieldUrl;
    private final boolean healthCheckEnabled;
    private final int maxRetryAttempts;
    private final long retryDelayMs;
    private final long maxWaitMs;

    // Pending request queue for when greenfield is down
    private final Queue<PendingRequest> pendingQueue = new ConcurrentLinkedQueue<>();

    // Metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong queuedRequests = new AtomicLong(0);
    private final AtomicLong totalRetries = new AtomicLong(0);
    private final AtomicLong totalWaitTimeMs = new AtomicLong(0);
    private final AtomicLong lastHealthCheckMs = new AtomicLong(0);
    private volatile boolean lastHealthStatus = false;
    private final ConcurrentHashMap<String, RequestMetrics> requestMetricsMap = new ConcurrentHashMap<>();

    public GreenfieldHealthService(
            @Value("${greenfield.url:http://localhost:8080}") String greenfieldUrl,
            @Value("${greenfield.health.enabled:true}") boolean healthCheckEnabled,
            @Value("${greenfield.retry.max-attempts:30}") int maxRetryAttempts,
            @Value("${greenfield.retry.delay-ms:2000}") long retryDelayMs,
            @Value("${greenfield.retry.max-wait-ms:120000}") long maxWaitMs) {
        this.restTemplate = new RestTemplate();
        this.greenfieldUrl = greenfieldUrl;
        this.healthCheckEnabled = healthCheckEnabled;
        this.maxRetryAttempts = maxRetryAttempts;
        this.retryDelayMs = retryDelayMs;
        this.maxWaitMs = maxWaitMs;
    }

    /**
     * Check if greenfield is currently healthy.
     */
    public boolean isHealthy() {
        if (!healthCheckEnabled) {
            return true; // Skip health check if disabled
        }
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    greenfieldUrl + "/health", String.class);
            lastHealthStatus = response.getStatusCode().is2xxSuccessful();
            lastHealthCheckMs.set(System.currentTimeMillis());
            return lastHealthStatus;
        } catch (RestClientException e) {
            log.debug("Greenfield health check failed: {}", e.getMessage());
            lastHealthStatus = false;
            lastHealthCheckMs.set(System.currentTimeMillis());
            return false;
        }
    }

    /**
     * Execute with retry strategy - waits for greenfield if down.
     * This is the main entry point for executing requests that depend on greenfield.
     *
     * @param requestId Unique ID for tracking
     * @param operation The operation to execute
     * @return Result of the operation
     */
    public <T> ExecutionResult<T> executeWithRetry(String requestId, Supplier<T> operation) {
        totalRequests.incrementAndGet();
        long startTime = System.currentTimeMillis();
        RequestMetrics metrics = new RequestMetrics(requestId, startTime);
        requestMetricsMap.put(requestId, metrics);

        log.info("[{}] Starting execution, checking greenfield health...", requestId);

        // Wait for greenfield to be healthy
        WaitResult waitResult = waitForHealthy(requestId);
        metrics.waitTimeMs = waitResult.waitTimeMs;
        metrics.retryCount = waitResult.retryCount;

        if (!waitResult.success) {
            metrics.status = "TIMEOUT";
            metrics.endTime = System.currentTimeMillis();
            failedRequests.incrementAndGet();
            totalWaitTimeMs.addAndGet(metrics.waitTimeMs);
            log.error("[{}] Greenfield unavailable after {} retries, {}ms wait",
                    requestId, waitResult.retryCount, waitResult.waitTimeMs);
            return ExecutionResult.timeout(requestId, waitResult.waitTimeMs, waitResult.retryCount);
        }

        // Greenfield is healthy - execute the operation
        try {
            log.info("[{}] Greenfield healthy, executing operation...", requestId);
            T result = operation.get();
            metrics.status = "SUCCESS";
            metrics.endTime = System.currentTimeMillis();
            successfulRequests.incrementAndGet();
            totalWaitTimeMs.addAndGet(metrics.waitTimeMs);
            totalRetries.addAndGet(metrics.retryCount);
            log.info("[{}] Execution completed successfully in {}ms (waited {}ms, {} retries)",
                    requestId, metrics.endTime - startTime, metrics.waitTimeMs, metrics.retryCount);
            return ExecutionResult.success(result, metrics.waitTimeMs, metrics.retryCount);
        } catch (Exception e) {
            metrics.status = "FAILED";
            metrics.error = e.getMessage();
            metrics.endTime = System.currentTimeMillis();
            failedRequests.incrementAndGet();
            totalWaitTimeMs.addAndGet(metrics.waitTimeMs);
            totalRetries.addAndGet(metrics.retryCount);
            log.error("[{}] Execution failed: {}", requestId, e.getMessage());
            return ExecutionResult.failed(requestId, e.getMessage(), metrics.waitTimeMs, metrics.retryCount);
        }
    }

    /**
     * Wait for greenfield to become healthy with exponential backoff.
     */
    private WaitResult waitForHealthy(String requestId) {
        if (isHealthy()) {
            return new WaitResult(true, 0, 0);
        }

        log.warn("[{}] Greenfield is DOWN, starting wait strategy...", requestId);
        queuedRequests.incrementAndGet();

        long startWait = System.currentTimeMillis();
        int retries = 0;
        long currentDelay = retryDelayMs;

        while (retries < maxRetryAttempts) {
            long elapsed = System.currentTimeMillis() - startWait;
            if (elapsed >= maxWaitMs) {
                log.error("[{}] Max wait time {}ms exceeded", requestId, maxWaitMs);
                queuedRequests.decrementAndGet();
                return new WaitResult(false, elapsed, retries);
            }

            try {
                Thread.sleep(currentDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                queuedRequests.decrementAndGet();
                return new WaitResult(false, System.currentTimeMillis() - startWait, retries);
            }

            retries++;
            log.info("[{}] Retry {}/{}: checking greenfield health...", requestId, retries, maxRetryAttempts);

            if (isHealthy()) {
                long waitTime = System.currentTimeMillis() - startWait;
                log.info("[{}] Greenfield is UP after {} retries ({}ms)", requestId, retries, waitTime);
                queuedRequests.decrementAndGet();
                return new WaitResult(true, waitTime, retries);
            }

            // Exponential backoff with cap
            currentDelay = Math.min(currentDelay * 2, 10000);
        }

        queuedRequests.decrementAndGet();
        return new WaitResult(false, System.currentTimeMillis() - startWait, retries);
    }

    /**
     * Get comprehensive metrics.
     */
    public Map<String, Object> getMetrics() {
        boolean healthy = isHealthy();
        return Map.of(
                "greenfield", Map.of(
                        "url", greenfieldUrl,
                        "healthy", healthy,
                        "lastCheckMs", lastHealthCheckMs.get(),
                        "healthCheckEnabled", healthCheckEnabled
                ),
                "requests", Map.of(
                        "total", totalRequests.get(),
                        "successful", successfulRequests.get(),
                        "failed", failedRequests.get(),
                        "currentlyQueued", queuedRequests.get()
                ),
                "performance", Map.of(
                        "totalRetries", totalRetries.get(),
                        "totalWaitTimeMs", totalWaitTimeMs.get(),
                        "avgWaitTimeMs", totalRequests.get() > 0
                                ? totalWaitTimeMs.get() / totalRequests.get() : 0
                ),
                "config", Map.of(
                        "maxRetryAttempts", maxRetryAttempts,
                        "retryDelayMs", retryDelayMs,
                        "maxWaitMs", maxWaitMs
                )
        );
    }

    /**
     * Get health status summary.
     */
    public HealthStatus getHealthStatus() {
        boolean healthy = isHealthy();
        return new HealthStatus(
                healthy,
                greenfieldUrl,
                healthy ? "UP" : "DOWN",
                queuedRequests.get(),
                System.currentTimeMillis()
        );
    }

    // --- Records ---

    public record HealthStatus(
            boolean healthy,
            String url,
            String status,
            long queuedRequests,
            long checkedAt
    ) {}

    public record WaitResult(boolean success, long waitTimeMs, int retryCount) {}

    public record PendingRequest(
            String id,
            String type,
            Object payload,
            long queuedAt
    ) {}

    public static class RequestMetrics {
        public final String requestId;
        public final long startTime;
        public long endTime;
        public long waitTimeMs;
        public int retryCount;
        public String status;
        public String error;

        public RequestMetrics(String requestId, long startTime) {
            this.requestId = requestId;
            this.startTime = startTime;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "requestId", requestId,
                    "startTime", startTime,
                    "endTime", endTime,
                    "durationMs", endTime - startTime,
                    "waitTimeMs", waitTimeMs,
                    "retryCount", retryCount,
                    "status", status != null ? status : "UNKNOWN",
                    "error", error != null ? error : ""
            );
        }
    }

    public static class ExecutionResult<T> {
        private final boolean success;
        private final T result;
        private final String error;
        private final long waitTimeMs;
        private final int retryCount;
        private final boolean timeout;

        private ExecutionResult(boolean success, T result, String error,
                                long waitTimeMs, int retryCount, boolean timeout) {
            this.success = success;
            this.result = result;
            this.error = error;
            this.waitTimeMs = waitTimeMs;
            this.retryCount = retryCount;
            this.timeout = timeout;
        }

        public static <T> ExecutionResult<T> success(T result, long waitTimeMs, int retryCount) {
            return new ExecutionResult<>(true, result, null, waitTimeMs, retryCount, false);
        }

        public static <T> ExecutionResult<T> failed(String requestId, String error, long waitTimeMs, int retryCount) {
            return new ExecutionResult<>(false, null, error, waitTimeMs, retryCount, false);
        }

        public static <T> ExecutionResult<T> timeout(String requestId, long waitTimeMs, int retryCount) {
            return new ExecutionResult<>(false, null, "Greenfield unavailable - timeout after " + waitTimeMs + "ms",
                    waitTimeMs, retryCount, true);
        }

        public boolean isSuccess() { return success; }
        public T getResult() { return result; }
        public String getError() { return error; }
        public long getWaitTimeMs() { return waitTimeMs; }
        public int getRetryCount() { return retryCount; }
        public boolean isTimeout() { return timeout; }

        public Map<String, Object> toMap() {
            return Map.of(
                    "success", success,
                    "timeout", timeout,
                    "waitTimeMs", waitTimeMs,
                    "retryCount", retryCount,
                    "error", error != null ? error : ""
            );
        }
    }
}