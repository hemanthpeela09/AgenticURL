package com.example.app;

import com.example.orchestrator.core.GreenfieldHealthService;
import com.example.orchestrator.llm.UrlShortenerAgent;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrchestratorWorkflowService {

    private final UrlShortenerAgent urlShortenerAgent;
    private final GreenfieldHealthService greenfieldHealthService;

    public OrchestratorWorkflowService(UrlShortenerAgent urlShortenerAgent,
                                       GreenfieldHealthService greenfieldHealthService) {
        this.urlShortenerAgent = urlShortenerAgent;
        this.greenfieldHealthService = greenfieldHealthService;
    }

    public Map<String, Object> shorten(String prompt) {
        var result = greenfieldHealthService.executeWithRetry(
                UUID.randomUUID().toString(),
                () -> urlShortenerAgent.processPrompt(prompt));

        Map<String, Object> payload = new LinkedHashMap<>();
        if (result.isSuccess()) {
            UrlShortenerAgent.ShortenResult shortenResult = result.getResult();
            payload.put("success", shortenResult.success());
            payload.put("code", shortenResult.code());
            payload.put("shortUrl", shortenResult.shortUrl());
            payload.put("originalUrl", shortenResult.originalUrl());
            payload.put("customAlias", shortenResult.customAlias());
            payload.put("ttlSeconds", shortenResult.ttlSeconds());
            payload.put("message", shortenResult.message());
            payload.put("error", shortenResult.error());
        } else {
            payload.put("success", false);
            payload.put("code", null);
            payload.put("shortUrl", null);
            payload.put("originalUrl", null);
            payload.put("customAlias", null);
            payload.put("ttlSeconds", null);
            payload.put("message", null);
            payload.put("error", result.getError());
        }
        payload.put("workflow", "langchain-agent");
        payload.put("scheduler", "dag-orchestrator");
        payload.put("waitTimeMs", result.getWaitTimeMs());
        payload.put("retryCount", result.getRetryCount());
        payload.put("timeout", result.isTimeout());
        return payload;
    }
}
