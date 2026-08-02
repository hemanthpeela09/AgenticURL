package com.example.orchestrator.llm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for LLM operations.
 * Provides endpoints for chat, chains, and agent interactions.
 */
@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmService llmService;
    private final UrlShortenerAgent urlShortenerAgent;

    public LlmController(LlmService llmService, UrlShortenerAgent urlShortenerAgent) {
        this.llmService = llmService;
        this.urlShortenerAgent = urlShortenerAgent;
    }

    /**
     * Get LLM backend information.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        return ResponseEntity.ok(Map.of(
                "backend", llmService.getBackendName(),
                "supportsStreaming", llmService.supportsStreaming()
        ));
    }


    /**
     * Execute an SDLC chain.
     */
    @PostMapping("/chain/sdlc")
    public ResponseEntity<Map<String, Object>> executeSdlcChain(@RequestBody SdlcChainRequest request) {
        Map<String, Object> result = llmService.sdlcChain(request.requirement()).execute();
        return ResponseEntity.ok(result);
    }

    /**
     * Run an SDLC agent.
     */
    @PostMapping("/agent/sdlc")
    public ResponseEntity<AgentResponse> runSdlcAgent(@RequestBody AgentRequest request) {
        AgentExecutor.AgentResult result = llmService.sdlcAgent().run(request.input());
        return ResponseEntity.ok(new AgentResponse(
                result.output(),
                result.success(),
                result.steps().stream()
                        .map(s -> new AgentStepResponse(s.thought(), s.action(), s.actionInput(), s.observation()))
                        .toList()
        ));
    }

    /**
     * Process a natural language prompt to shorten a URL.
     * This uses AI to understand the user's intent and extract URL/alias from the prompt.
     *
     * Example prompts:
     * - "Shorten https://example.com/long/url with alias mylink"
     * - "Create a short URL for https://google.com"
     * - "I want to shorten https://github.com/repo and call it gh-repo"
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenAgentResponse> shortenWithAi(@RequestBody ShortenAgentRequest request) {
        UrlShortenerAgent.ShortenResult result = urlShortenerAgent.processPrompt(request.prompt());

        return ResponseEntity.ok(new ShortenAgentResponse(
                result.success(),
                result.code(),
                result.shortUrl(),
                result.originalUrl(),
                result.customAlias(),
                result.ttlSeconds(),
                result.message(),
                result.error()
        ));
    }

    // ---- Request/Response DTOs ----
    public record SdlcChainRequest(String requirement) {}

    public record AgentRequest(String input) {}

    public record AgentResponse(
            String output,
            boolean success,
            List<AgentStepResponse> steps
    ) {}

    public record AgentStepResponse(
            String thought,
            String action,
            String actionInput,
            String observation
    ) {}

    public record ShortenAgentRequest(String prompt) {}

    public record ShortenAgentResponse(
            boolean success,
            String code,
            String shortUrl,
            String originalUrl,
            String customAlias,
            Long ttlSeconds,
            String message,
            String error
    ) {}
}