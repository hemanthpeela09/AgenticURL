package com.example.orchestrator.metrics;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reliability + performance metrics for a run: success rate, retry/rollback
 * frequency, MTTR (mean time to recovery), and end-to-end latency.
 */
public class Metrics {

    private final AtomicInteger nodeAttempts = new AtomicInteger();
    private final AtomicInteger nodeSuccesses = new AtomicInteger();
    private final AtomicInteger nodeFailures = new AtomicInteger();
    private final AtomicInteger retries = new AtomicInteger();
    private final AtomicInteger rollbacks = new AtomicInteger();
    private final AtomicInteger safeStops = new AtomicInteger();
    private final AtomicInteger approvalsRequested = new AtomicInteger();
    private final AtomicInteger replans = new AtomicInteger();

    private final List<Long> recoveryTimes = new CopyOnWriteArrayList<>();
    private final Map<String, Long> pendingFailureTs = new ConcurrentHashMap<>();

    private volatile long startedAt = System.currentTimeMillis();
    private volatile long endedAt = 0L;

    public void start() { startedAt = System.currentTimeMillis(); }

    public void stop() { endedAt = System.currentTimeMillis(); }

    public void recordAttempt() { nodeAttempts.incrementAndGet(); }

    public void recordFailure(String node) {
        nodeFailures.incrementAndGet();
        pendingFailureTs.putIfAbsent(node, System.currentTimeMillis());
    }

    public void recordSuccess(String node) {
        nodeSuccesses.incrementAndGet();
    }

    public void recordRetry() { retries.incrementAndGet(); }

    public void recordRollback() { rollbacks.incrementAndGet(); }

    public void recordSafeStop() { safeStops.incrementAndGet(); }

    public void recordApproval() { approvalsRequested.incrementAndGet(); }

    public void recordReplan() { replans.incrementAndGet(); }

    public double successRate() {
        int total = nodeSuccesses.get() + nodeFailures.get();
        return total == 0 ? 1.0 : round((double) nodeSuccesses.get() / total);
    }

    public double retryFrequency() {
        int a = nodeAttempts.get();
        return a == 0 ? 0.0 : round((double) retries.get() / a);
    }

    public double rollbackFrequency() {
        int a = nodeAttempts.get();
        return a == 0 ? 0.0 : round((double) rollbacks.get() / a);
    }

    public double mttrSeconds() {
        if (recoveryTimes.isEmpty()) {
            return 0.0;
        }
        long sum = recoveryTimes.stream().mapToLong(Long::longValue).sum();
        return round((sum / (double) recoveryTimes.size()) / 1000.0);
    }

    public double e2eLatencySeconds() {
        long end = endedAt == 0L ? System.currentTimeMillis() : endedAt;
        return round((end - startedAt) / 1000.0);
    }

    public int replans() { return replans.get(); }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("successRate", successRate());
        m.put("nodeAttempts", nodeAttempts.get());
        m.put("nodeSuccesses", nodeSuccesses.get());
        m.put("nodeFailures", nodeFailures.get());
        m.put("retries", retries.get());
        m.put("retryFrequency", retryFrequency());
        m.put("rollbacks", rollbacks.get());
        m.put("rollbackFrequency", rollbackFrequency());
        m.put("safeStops", safeStops.get());
        m.put("approvalsRequested", approvalsRequested.get());
        m.put("replans", replans.get());
        m.put("mttrSeconds", mttrSeconds());
        m.put("e2eLatencySeconds", e2eLatencySeconds());
        return m;
    }

    private static double round(double v) { return Math.round(v * 10000.0) / 10000.0; }
}