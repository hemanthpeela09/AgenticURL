package com.example.orchestrator.core;

/** A single audit-grade event in the decision lineage. */
public record LineageEvent(
        long sequence,
        long timestampMillis,
        String node,
        String kind,
        String detail,
        java.util.Map<String, Object> data) {
}
