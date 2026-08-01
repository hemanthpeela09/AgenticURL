package com.example.orchestrator.policy;

import com.example.orchestrator.core.Artifact;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Default guardrail set (G-1..G-3 in specs/04-governance-policy.spec.md). */
public final class Guardrails {

    private Guardrails() {
    }

    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("(?i)(api[_-]?key|secret|password|token)\\s*[:=]\\s*['\"]?[^'\"\\s]{6,}"),
            Pattern.compile("AKIA[0-9A-Z]{16}"),
            Pattern.compile("-----BEGIN (RSA|EC )?PRIVATE KEY-----")
    );

    private static final Set<String> HIGH_IMPACT = Set.of("design", "release");

    /** G-1: generated artifacts must not contain hardcoded secrets. */
    public static final Guardrail NO_HARDCODED_SECRETS = (node, artifact, bb) -> {
        if (artifact == null) {
            return PolicyResult.allow("no artifact to scan");
        }
        String text = String.valueOf(artifact.content());
        for (Pattern p : SECRET_PATTERNS) {
            if (p.matcher(text).find()) {
                return PolicyResult.block("hardcoded secret detected in '" + artifact.name() + "'");
            }
        }
        return PolicyResult.allow("no secrets found");
    };

    /** G-2: change control - schema/design changes and releases need approval. */
    public static final Guardrail HIGH_IMPACT_NEEDS_APPROVAL = (node, artifact, bb) ->
            HIGH_IMPACT.contains(node)
                    ? PolicyResult.needsApproval("'" + node + "' is a high-impact change")
                    : PolicyResult.allow("standard-impact change");

    /** G-3: compliance - no release unless a passing test report exists. */
    public static final Guardrail TESTS_MUST_PASS_BEFORE_RELEASE = (node, artifact, bb) -> {
        if (!"release".equals(node)) {
            return PolicyResult.allow("not a release node");
        }
        Artifact report = bb.get("test_report");
        if (report == null) {
            return PolicyResult.block("release blocked: no test report");
        }
        Object content = report.content();
        boolean passed = content instanceof java.util.Map<?, ?> m
                && Boolean.TRUE.equals(m.get("passed"));
        return passed
                ? PolicyResult.allow("tests passed; release permitted")
                : PolicyResult.block("release blocked: tests failing");
    };

    /**
     * G-4: brownfield change control - a design that flags a breaking change to
     * existing contracts must not proceed without an explicit data-migration
     * approval. This blocks the design node and safe-stops the run.
     */
    public static final Guardrail BROWNFIELD_CHANGE_CONTROL = (node, artifact, bb) -> {
        if (!"design".equals(node)) {
            return PolicyResult.allow("not a design node");
        }
        Artifact design = bb.get("design_spec");
        if (design == null || !(design.content() instanceof java.util.Map<?, ?> spec)) {
            return PolicyResult.allow("no design to evaluate yet");
        }
        if (spec.get("impactAnalysis") instanceof java.util.Map<?, ?> impact
                && Boolean.TRUE.equals(impact.get("breakingChange"))
                && !bb.isApproved("__migration_approved__")) {
            return PolicyResult.block("change-control: breaking change to existing contracts "
                    + impact.get("changedContracts")
                    + " requires a data-migration approval, which was not granted");
        }
        return PolicyResult.allow("no un-approved breaking change");
    };

    public static List<Guardrail> defaults() {
        return List.of(
                NO_HARDCODED_SECRETS,
                HIGH_IMPACT_NEEDS_APPROVAL,
                BROWNFIELD_CHANGE_CONTROL,
                TESTS_MUST_PASS_BEFORE_RELEASE
        );
    }
}