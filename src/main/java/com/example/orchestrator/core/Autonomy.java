package com.example.orchestrator.core;

/** Autonomy boundary for a node (controlled autonomy). */
public enum Autonomy {
    /** Agent acts without human sign-off. */
    AUTO,

    /** High-impact: a human approval checkpoint is required before running. */
    NEEDS_APPROVAL,

    /** Never auto-executes. */
    BLOCKED
}