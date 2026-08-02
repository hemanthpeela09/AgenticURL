package com.example.app;

import com.example.orchestrator.core.ApprovalStrategy;
import com.example.orchestrator.core.GreenfieldHealthService;
import com.example.orchestrator.llm.LlmFactory;
import com.example.orchestrator.scenario.Scenario;
import com.example.orchestrator.scenario.ScenarioRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/** Exposes orchestrator runs to the React UI / API clients. */
@RestController
@RequestMapping("/api/orchestrator")
public class OrchestratorController {

    private final GreenfieldHealthService healthService;
    private final OrchestratorWorkflowService workflowService;

    public OrchestratorController(GreenfieldHealthService healthService, OrchestratorWorkflowService workflowService){
        this.healthService = healthService;
        this.workflowService = workflowService;
    }

    @GetMapping("/scenarios")
    public List<Map<String, String>> scenarios() {
        return Arrays.stream(Scenario.values())
                .map(s -> Map.of("key", s.key(), "requirement", s.requirement()))
                .toList();
    }

    @PostMapping("/run")
    public Object run(@RequestParam(defaultValue = "greenfield") String scenario,
                      @RequestParam(defaultValue = "auto") String approval) {
        Scenario s = Scenario.fromKey(scenario);
        ApprovalStrategy strategy = "deny".equalsIgnoreCase(approval)
                ? ApprovalStrategy.denyAll()
                : ApprovalStrategy.autoApprove();
        ScenarioRunner runner = new ScenarioRunner(LlmFactory.fromEnv(), (String k, Map<String, Object> d) -> { });
        return runner.run(s, strategy);
    }

    @GetMapping("/greenfield/health")
    public GreenfieldHealthService.HealthStatus greenfieldHealth() {
        return healthService.getHealthStatus();
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return healthService.getMetrics();
    }

    @PostMapping("/shorten")
    public ResponseEntity<Map<String, Object>> shorten(@RequestBody Map<String, Object> request) {
        Object promptValue = request.get("prompt");
        String prompt = promptValue == null ? "" : String.valueOf(promptValue).trim();

        if (prompt.isBlank()) {
            String url = request.get("url") == null ? "" : String.valueOf(request.get("url")).trim();
            String customAlias = request.get("customAlias") == null ? "" : String.valueOf(request.get("customAlias")).trim();
            String ttlSeconds = request.get("ttlSeconds") == null ? "" : String.valueOf(request.get("ttlSeconds")).trim();

            StringBuilder builder = new StringBuilder();
            if (!url.isBlank()) {
                builder.append("Shorten ").append(url);
            }
            if (!customAlias.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append("with alias ").append(customAlias);
            }
            if (!ttlSeconds.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append("with ttl ").append(ttlSeconds);
            }
            prompt = builder.toString();
        }

        return ResponseEntity.ok(workflowService.shorten(prompt));
    }
}
