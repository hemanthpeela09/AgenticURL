package com.example.orchestrator.scenario;

import com.example.orchestrator.agents.Agents;
import com.example.orchestrator.core.Autonomy;
import com.example.orchestrator.core.Blackboard;
import com.example.orchestrator.core.Gate;
import com.example.orchestrator.core.Gates;
import com.example.orchestrator.core.Node;

import java.util.List;

/**
 * Builds the SDLC dependency graph from specs/03-orchestration.spec.md.
 * Non-linear: implementation and test_authoring run in parallel then join
 * before testing; design and release require human approval.
 */
public final class SdlcGraph {

    private SdlcGraph() {
    }

    public static List<Node> build(Agents agents) {
        Node requirements = Node.builder("requirements", agents.analyst())
                .exitGate(Gates.produces("requirement_spec"))
                .build();

        // The design node cannot start while requirement ambiguities are open.
        // Because 'requirements' succeeds (and is not rolled back), the open
        // questions persist for a human to resolve; design safe-stops until then.
        Node design = Node.builder("design", agents.architect())
                .dependsOn("requirements")
                .entryGate(Gates.all("design_entry",
                        Gates.requires("requirement_spec"), Gates.noOpenQuestions()))
                .exitGate(Gates.produces("design_spec"))
                .autonomy(Autonomy.NEEDS_APPROVAL)
                .build();

        Node implementation = Node.builder("implementation", agents.implementer())
                .dependsOn("design")
                .entryGate(Gates.requires("design_spec"))
                .exitGate(Gates.produces("code_manifest"))
                .parallelGroup("build")
                .build();

        Node testAuthoring = Node.builder("test_authoring", agents.testAuthor())
                .dependsOn("design")
                .entryGate(Gates.requires("design_spec"))
                .exitGate(Gates.produces("test_plan"))
                .parallelGroup("build")
                .build();

        Node testing = Node.builder("testing", agents.tester())
                .dependsOn("implementation", "test_authoring")
                .entryGate(Gates.requires("code_manifest", "test_plan"))
                .exitGate(Gates.all("testing_exit",
                        Gates.produces("test_report"),
                        new Gate("tests_passed", "test report must pass",
                                bb -> {
                                    var r = bb.get("test_report");
                                    return r != null && r.content() instanceof java.util.Map<?, ?> m
                                            && Boolean.TRUE.equals(m.get("passed"));
                                })))
                .build();

        Node documentation = Node.builder("documentation", agents.docWriter())
                .dependsOn("testing")
                .entryGate(Gates.requires("test_report"))
                .exitGate(Gates.produces("docs"))
                .build();

        Node release = Node.builder("release", agents.release())
                .dependsOn("testing", "documentation")
                .entryGate(Gates.requires("test_report", "docs"))
                .exitGate(Gates.produces("release_record"))
                .autonomy(Autonomy.NEEDS_APPROVAL)
                .build();

        return List.of(requirements, design, implementation, testAuthoring,
                testing, documentation, release);
    }
}