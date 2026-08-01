package com.example.orchestrator.agents;

import com.example.orchestrator.core.Agent;
import com.example.orchestrator.core.Artifact;
import com.example.orchestrator.llm.Llm;

public class Agents {
    private final Llm llm;

    public Agents(Llm llm) {
        this.llm = llm;
    }

    /** Analyst: normalize the requirement + surface ambiguities as open questions. */
    public Agent analyst() {
        return bb -> {
            String req = bb.requirement();
            llm.complete("You are a requirements analyst.", "Normalize: " + req);
            java.util.Map<String, Object> spec = new java.util.LinkedHashMap<>();
            spec.put("intent", req);
            spec.put("functional", java.util.List.of(
                    "create short link", "redirect", "analytics", "custom alias", "ttl expiry"));
            spec.put("nonFunctional", java.util.List.of("modular", "testable", "observable", "secure"));

            java.util.List<String> ambiguities = detectAmbiguities(req);
            spec.put("ambiguities", ambiguities);
            // Unresolved ambiguities become open questions -> requirements exit gate blocks.
            if (!bb.isApproved("__clarified__")) {
                ambiguities.forEach(bb::addOpenQuestion);
            }
            bb.put(new Artifact("requirement_spec", Artifact.Kind.REQUIREMENT, "requirements", spec));
        };
    }

    /** Architect: components, endpoints, data flows; impact analysis for brownfield. */
    public Agent architect() {
        return bb -> {
            llm.complete("You are a software architect.", "Design for: " + bb.requirement());
            java.util.Map<String, Object> design = new java.util.LinkedHashMap<>();
            design.put("components", java.util.List.of(
                    "api (controllers)", "service (LinkService)", "store (LinkStore)",
                    "domain (Link, Click, ShortCode)", "rate-limiter"));
            design.put("endpoints", java.util.List.of(
                    "POST /api/shorten", "GET /{code}", "GET /api/stats/{code}", "GET /health"));
            design.put("dataFlows", java.util.List.of(
                    "shorten -> validate -> store.save",
                    "redirect -> store.get -> record click -> 302",
                    "stats -> aggregate clicks"));
            if (isBrownfield(bb.requirement())) {
                // Brownfield touches the *existing* production service: it mutates
                // persisted contracts (Link schema + Stats response), so under
                // change-control this is a breaking change requiring an explicit
                // data-migration sign-off (see G-4). Without that approval the
                // design node is blocked and the run safe-stops.
                design.put("impactAnalysis", new java.util.LinkedHashMap<>(java.util.Map.of(
                        "impactedModules", java.util.List.of("store", "service.LinkService", "api.StatsController"),
                        "changedContracts", java.util.List.of("Link.expiresAt", "StatsResponse.clicksByDay"),
                        "breakingChange", true,
                        "requiresChangeApproval", true,
                        "risk", "modifies existing persisted Link + Stats contracts; "
                                + "requires data migration and change-control sign-off")));
            }
            bb.put(new Artifact("design_spec", Artifact.Kind.DESIGN, "design", design));
        };
    }

    /** Implementer: produce a code manifest for the planned change. */
    public Agent implementer() {
        return bb -> {
            llm.complete("You are an implementer.", "Implement per design.");
            java.util.Map<String, Object> manifest = new java.util.LinkedHashMap<>();
            manifest.put("modules", java.util.List.of(
                    "com.example.shortener.api", "com.example.shortener.service",
                    "com.example.shortener.store", "com.example.shortener.domain"));
            manifest.put("classes", java.util.List.of(
                    "ShortenController", "RedirectController", "LinkService",
                    "InMemoryLinkStore", "ShortCode", "RateLimiter"));
            manifest.put("linesOfCode", 620);
            bb.put(new Artifact("code_manifest", Artifact.Kind.CODE, "implementation", manifest));
        };
    }

    /** Test author: derive a test plan from acceptance criteria. */
    public Agent testAuthor() {
        return bb -> {
            llm.complete("You are a test engineer.", "Author tests.");
            java.util.Map<String, Object> plan = new java.util.LinkedHashMap<>();
            plan.put("unit", java.util.List.of("ShortCodeTest", "RateLimiterTest", "MetricsTest", "GatesTest"));
            plan.put("contract", java.util.List.of("ShortenControllerTest", "RedirectControllerTest"));
            plan.put("integration", java.util.List.of("ScenarioIntegrationTest"));
            plan.put("cases", java.util.List.of("happy-path", "boundary", "error/exception"));
            bb.put(new Artifact("test_plan", Artifact.Kind.TESTS, "test_authoring", plan));
        };
    }

    /** Tester: execute the plan and produce a report (always passes in the mock path). */
    public Agent tester() {
        return bb -> {
            llm.complete("You are a QA engineer.", "Run tests.");
            Object plan = bb.get("test_plan") == null ? java.util.Map.of() : bb.get("test_plan").content();
            int total = countCases(plan);
            java.util.Map<String, Object> report = new java.util.LinkedHashMap<>();
            report.put("total", total);
            report.put("passed", true);
            report.put("failures", 0);
            report.put("coverage", "0.82");
            bb.put(new Artifact("test_report", Artifact.Kind.REPORT, "testing", report));
        };
    }

    /** Doc writer: produce documentation artifact. */
    public Agent docWriter() {
        return bb -> {
            llm.complete("You are a technical writer.", "Document the service.");
            java.util.Map<String, Object> docs = new java.util.LinkedHashMap<>();
            docs.put("sections", java.util.List.of("Overview", "API", "Setup", "Testing", "Limitations"));
            docs.put("openApi", "specs/02-openapi.yaml");
            bb.put(new Artifact("docs", Artifact.Kind.DOCS, "documentation", docs));
        };
    }

    /** Release: simulate a release record; no real deploy in this prototype. */
    public Agent release() {
        return bb -> {
            llm.complete("You are a release manager.", "Assess release readiness.");
            java.util.Map<String, Object> record = new java.util.LinkedHashMap<>();
            record.put("version", "1.0.0");
            record.put("status", "release-ready (simulated; no real deploy)");
            record.put("gatedBy", java.util.List.of("human approval", "tests passing"));
            bb.put(new Artifact("release_record", Artifact.Kind.REPORT, "release", record));
        };
    }

    // ---- helpers -----------------------------------------------------------

    private static int countCases(Object plan) {
        if (plan instanceof java.util.Map<?, ?> m) {
            int n = 0;
            for (Object v : m.values()) {
                if (v instanceof java.util.List<?> l) {
                    n += l.size();
                }
            }
            return n;
        }
        return 0;
    }

    private static boolean isBrownfield(String req) {
        String r = req.toLowerCase();
        return r.contains("existing") || r.contains("add ") || r.contains("enhance")
                || r.contains("refactor") || r.contains("bug");
    }

    private static java.util.ArrayList<String> detectAmbiguities(String req) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        String r = req.toLowerCase();
        if (r.contains("safe") || r.contains("secure") || r.contains("better")
                || r.contains("improve") || r.contains("robust")) {
            out.add("What does 'safer/better' mean here — malicious-URL scanning, auth, or rate limits?");
            out.add("Should existing links be affected or only new ones?");
            out.add("Is link expiry (TTL) required as part of this change?");
        }
        return out;
    }
}
