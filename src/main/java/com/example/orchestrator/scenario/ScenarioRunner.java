package com.example.orchestrator.scenario;

import com.example.orchestrator.agents.Agents;
import com.example.orchestrator.core.ApprovalStrategy;
import com.example.orchestrator.core.Blackboard;
import com.example.orchestrator.core.Node;
import com.example.orchestrator.core.Orchestrator;
import com.example.orchestrator.llm.Llm;
import com.example.orchestrator.llm.LlmFactory;
import com.example.orchestrator.policy.PolicyEngine;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Wires agents + graph + governance and runs a scenario end-to-end. */
public class ScenarioRunner {

    private final Llm llm;
    private final BiConsumer<String, Map<String, Object>> onEvent;

    public ScenarioRunner(Llm llm, BiConsumer<String, Map<String, Object>> onEvent) {
        this.llm = llm;
        this.onEvent = onEvent;
    }

    public ScenarioRunner() { this(LlmFactory.fromEnv(), (k, d) -> { }); }

    public RunResult run(Scenario scenario) { return run(scenario, ApprovalStrategy.autoApprove()); }

    public RunResult run(Scenario scenario, ApprovalStrategy approval) {
        Agents agents = new Agents(llm);
        List<Node> nodes = SdlcGraph.build(agents);
        Orchestrator orchestrator = new Orchestrator(nodes, approval, new PolicyEngine(), onEvent);
        Blackboard bb = new Blackboard(scenario.requirement());

        orchestrator.run(bb);

        // Ambiguous scenario: the requirements exit gate blocks on open questions.
        // Simulate a human answering the clarifications, then dynamically re-plan
        // and re-run the invalidated subgraph (demonstrates OR-8 / AC-15).
        if (scenario == Scenario.AMBIGUOUS && !bb.openQuestions().isEmpty()) {
            bb.log("human", "clarification",
                    "answered " + bb.openQuestions().size() + " open question(s)",
                    Map.of("resolution",
                            "scope = malicious-URL blocklist + TTL on new links only"));
            bb.setApproval("__clarified__", true);
            bb.clearOpenQuestions();
            // The requirement is revised, so re-run requirements and dynamically
            // re-plan the invalidated downstream subgraph (OR-8 / AC-15).
            bb.setStatus("requirements", com.example.orchestrator.core.NodeStatus.PENDING);
            bb.clearOutputHash("requirements");
            orchestrator.replanFrom(bb, "requirements");
            orchestrator.run(bb);
        }

        return RunResult.from(scenario.key(), bb, orchestrator.metrics().toMap());
    }
}