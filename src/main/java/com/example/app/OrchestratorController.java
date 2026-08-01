package com.example.app;

import com.example.orchestrator.core.ApprovalStrategy;
import com.example.orchestrator.llm.LlmFactory;
import com.example.orchestrator.scenario.Scenario;
import com.example.orchestrator.scenario.ScenarioRunner;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Exposes orchestrator runs to the React UI / API clients. */
@RestController
@RequestMapping("/api/orchestrator")
public class OrchestratorController {

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
}
