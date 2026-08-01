package com.example.orchestrator.policy;

import com.example.orchestrator.core.Artifact;
import com.example.orchestrator.core.Blackboard;

/** A security / compliance / change-control check over a node + its artifact. */
@FunctionalInterface
public interface Guardrail {
    /**
     * @param node     the node being evaluated
     * @param artifact the candidate artifact (nullable in the pre-phase)
     * @param bb       the run blackboard
     */
    PolicyResult evaluate(String node, Artifact artifact, Blackboard bb);
}
