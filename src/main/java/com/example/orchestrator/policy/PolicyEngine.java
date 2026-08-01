package com.example.orchestrator.policy;

import com.example.orchestrator.core.Artifact;
import com.example.orchestrator.core.Blackboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs the configured guardrails and records each evaluation to the lineage. */
public class PolicyEngine {

    private final List<Guardrail> guardrails;

    public PolicyEngine(List<Guardrail> guardrails) { this.guardrails = List.copyOf(guardrails); }

    public PolicyEngine() { this(Guardrails.defaults()); }

    public List<PolicyResult> evaluate(String node, Artifact artifact, Blackboard bb) {
        List<PolicyResult> results = new ArrayList<>();
        for (Guardrail g : guardrails) {
            PolicyResult r = g.evaluate(node, artifact, bb);
            results.add(r);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("allowed", r.allowed());
            data.put("requireApproval", r.requireApproval());
            data.put("severity", r.severity().name());
            bb.log(node, "policy", r.reason(), data);
        }
        return results;
    }
}