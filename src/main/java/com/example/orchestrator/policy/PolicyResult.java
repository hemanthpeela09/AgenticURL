package com.example.orchestrator.policy;

/** Outcome of a single guardrail evaluation. */
public record PolicyResult(
        boolean allowed,
        String reason,
        boolean requireApproval,
        Severity severity) {

    public enum Severity { INFO, WARN, HIGH }

    public static PolicyResult allow(String reason) { return new PolicyResult(true, reason, false, Severity.INFO); }

    public static PolicyResult block(String reason) { return new PolicyResult(false, reason, false, Severity.HIGH); }

    public static PolicyResult needsApproval(String reason) {
        return new PolicyResult(true, reason, true, Severity.HIGH);
    }
}