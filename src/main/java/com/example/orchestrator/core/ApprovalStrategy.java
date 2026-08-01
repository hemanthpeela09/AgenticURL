package com.example.orchestrator.core;

/** Human approval checkpoint for high-impact nodes (controlled autonomy). */
@FunctionalInterface
public interface ApprovalStrategy {

    boolean approve(String node, Blackboard bb);

    /** Non-interactive: approve + record (demo/CI default). */
    static ApprovalStrategy autoApprove() {
        return (String node, Blackboard bb) -> {
            bb.setApproval(node, true);
            return true;
        };
    }

    /** Reject everything: proves the safe-stop path. */
    static ApprovalStrategy denyAll() {
        return (String node, Blackboard bb) -> false;
    }
}
