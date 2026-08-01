package com.example.orchestrator.core;

public enum NodeStatus {
    PENDING,
    RUNNING,
    WAITING_APPROVAL,
    SUCCEEDED,
    FAILED,
    ROLLED_BACK,
    SKIPPED,
    SAFE_STOPPED
}
