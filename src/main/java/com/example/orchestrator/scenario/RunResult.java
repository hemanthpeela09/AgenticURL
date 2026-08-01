package com.example.orchestrator.scenario;

import com.example.orchestrator.core.Artifact;
import com.example.orchestrator.core.Blackboard;
import com.example.orchestrator.core.LineageEvent;
import com.example.orchestrator.core.NodeStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Serializable summary of an orchestrator run for the CLI/UI. */
public record RunResult(
        String runId,
        String scenario,
        String requirement,
        Map<String, String> nodeStatus,
        Map<String, Object> artifacts,
        List<String> openQuestions,
        Map<String, Boolean> approvals,
        List<Map<String, Object>> lineage,
        Map<String, Object> metrics,
        boolean completed,
        String safeStopReason) {

    public static RunResult from(String scenario, Blackboard bb, Map<String, Object> metrics) {
        Map<String, String> statuses = new LinkedHashMap<>();
        bb.statuses().forEach((k, v) -> statuses.put(k, v.name()));

        Map<String, Object> arts = new LinkedHashMap<>();
        for (Artifact a : bb.artifacts().values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", a.kind().name());
            m.put("producedBy", a.producedBy());
            m.put("version", a.version());
            m.put("hash", a.hash());
            m.put("content", a.content());
            arts.put(a.name(), m);
        }

        List<Map<String, Object>> lineage = bb.lineage().stream()
                .map(RunResult::event)
                .collect(Collectors.toList());

        boolean completed = bb.statuses().values().stream()
                .allMatch(s -> s == NodeStatus.SUCCEEDED);

        // Surface why a run stopped early: prefer the explicit safe_stop event,
        // else the first blocking policy evaluation.
        String safeStopReason = null;
        if (!completed) {
            safeStopReason = bb.lineage().stream()
                    .filter(e -> "safe_stop".equals(e.kind()))
                    .map(LineageEvent::detail)
                    .reduce((first, second) -> second)
                    .orElseGet(() -> bb.lineage().stream()
                            .filter(e -> "policy".equals(e.kind())
                                    && Boolean.FALSE.equals(e.data().get("allowed")))
                            .map(LineageEvent::detail)
                            .findFirst()
                            .orElse(null));
        }

        return new RunResult(bb.runId(), scenario, bb.requirement(), statuses, arts,
                List.copyOf(bb.openQuestions()), Map.copyOf(bb.approvals()), lineage, metrics,
                completed, safeStopReason);
    }

    private static Map<String, Object> event(LineageEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seq", e.sequence());
        m.put("node", e.node());
        m.put("kind", e.kind());
        m.put("detail", e.detail());
        m.put("data", e.data());
        return m;
    }
}