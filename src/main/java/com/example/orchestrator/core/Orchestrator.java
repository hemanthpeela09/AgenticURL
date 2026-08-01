package com.example.orchestrator.core;

import com.example.orchestrator.metrics.Metrics;
import com.example.orchestrator.policy.PolicyEngine;
import com.example.orchestrator.policy.PolicyResult;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * The agentic orchestration engine: an explicit dependency-graph executor.
 * Non-linear, stateful, governed. See specs/03-orchestration.spec.md.
 */
public class Orchestrator {

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final ApprovalStrategy approval;
    private final PolicyEngine policy;
    private final Metrics metrics = new Metrics();
    private final BiConsumer<String, Map<String, Object>> onEvent;

    public Orchestrator(List<Node> nodes, ApprovalStrategy approval, PolicyEngine policy,
                        BiConsumer<String, Map<String, Object>> onEvent) {
        for (Node n : nodes) {
            this.nodes.put(n.id(), n);
        }
        this.approval = approval != null ? approval : ApprovalStrategy.autoApprove();
        this.policy = policy != null ? policy : new PolicyEngine();
        this.onEvent = onEvent != null ? onEvent : (String k, Map<String, Object> d) -> { };
        validateGraph();
    }

    public Orchestrator(List<Node> nodes) { this(nodes, ApprovalStrategy.autoApprove(), new PolicyEngine(), null); }

    public Metrics metrics() { return metrics; }

    // ---- graph validation ---------------------------------------------------
    private void validateGraph() {
        for (Node n : nodes.values()) {
            for (String dep : n.dependsOn()) {
                if (!nodes.containsKey(dep)) {
                    throw new IllegalArgumentException("node '" + n.id() + "' depends on unknown node '" + dep + "'");
                }
            }
        }
        topologicalOrder(); // throws on cycle
    }

    List<String> topologicalOrder() {
        Map<String, Integer> indeg = new LinkedHashMap<>();
        for (String id : nodes.keySet()) {
            indeg.put(id, 0);
        }
        for (Node n : nodes.values()) {
            indeg.merge(n.id(), n.dependsOn().size(), Integer::sum);
        }
        List<String> ready = new ArrayList<>();
        indeg.forEach((String id, Integer d) -> {
            if (d == 0) {
                ready.add(id);
            }
        });
        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            ready.sort(String::compareTo);
            String id = ready.remove(0);
            order.add(id);
            for (Node other : nodes.values()) {
                if (other.dependsOn().contains(id)) {
                    int d = indeg.merge(other.id(), -1, Integer::sum);
                    if (d == 0) {
                        ready.add(other.id());
                    }
                }
            }
        }
        if (order.size() != nodes.size()) {
            throw new IllegalArgumentException("dependency graph has a cycle");
        }
        return order;
    }

    // ---- execution ----------------------------------------------------------
    public Blackboard run(Blackboard bb) {
        metrics.start();
        for (Node n : nodes.values()) {
            if (bb.status(n.id()) == null) {
                bb.setStatus(n.id(), NodeStatus.PENDING);
            }
        }
        List<String> order = topologicalOrder();
        emit(bb, "run_start", Map.of("order", order));

        try {
            Set<String> executed = new HashSet<>();
            // nodes already SUCCEEDED (e.g. from a prior run before replan) count as executed
            for (String id : order) {
                if (bb.status(id) == NodeStatus.SUCCEEDED) {
                    executed.add(id);
                }
            }
            while (executed.size() < nodes.size()) {
                List<String> wave = new ArrayList<>();
                for (String id : order) {
                    if (!executed.contains(id)
                            && new HashSet<>(executed).containsAll(nodes.get(id).dependsOn())) {
                        wave.add(id);
                    }
                }
                if (wave.isEmpty()) {
                    break;
                }
                runWave(bb, wave, executed);
            }
        } catch (SafeStopException stop) {
            bb.log("orchestrator", "safe_stop", stop.getMessage());
            metrics.recordSafeStop();
            emit(bb, "safe_stop", Map.of("reason", stop.getMessage()));
        } finally {
            metrics.stop();
        }

        Map<String, Object> report = metrics.toMap();
        bb.log("orchestrator", "metrics", "final reliability metrics", report);
        emit(bb, "run_end", Map.of("metrics", report));
        return bb;
    }

    private void runWave(Blackboard bb, List<String> wave, Set<String> executed) {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        List<String> singles = new ArrayList<>();
        for (String id : wave) {
            String grp = nodes.get(id).parallelGroup();
            if (grp != null) {
                groups.computeIfAbsent(grp, k -> new ArrayList<>()).add(id);
            } else {
                singles.add(id);
            }
        }

        for (String id : singles) {
            executeNode(bb, id);
            executed.add(id);
        }

        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            List<String> members = e.getValue();
            if (members.size() == 1) {
                executeNode(bb, members.get(0));
            } else {
                bb.log("orchestrator", "parallel", "parallel group '" + e.getKey() + "'", Map.of("members", members));
                emit(bb, "parallel_start", Map.of("group", e.getKey(), "members", members));
                ExecutorService pool = Executors.newFixedThreadPool(members.size());
                try {
                    List<Future<?>> futures = members.stream()
                            .map(m -> pool.submit(() -> executeNode(bb, m)))
                            .collect(Collectors.toList());
                    for (Future<?> f : futures) {
                        awaitFuture(f);
                    }
                } finally {
                    pool.shutdownNow();
                }
                bb.log("orchestrator", "synchronize", "joined group '" + e.getKey() + "'", Map.of("members", members));
                emit(bb, "synchronize", Map.of("group", e.getKey(), "members", members));
            }
            executed.addAll(members);
        }
    }

    private void awaitFuture(Future<?> f) {
        try {
            f.get();
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SafeStopException sse) {
                throw sse;
            }
            throw new SafeStopException("parallel node failed: " + ex.getMessage());
        }
    }

    private void executeNode(Blackboard bb, String id) {
        Node node = nodes.get(id);
        bb.setStatus(id, NodeStatus.RUNNING);
        emit(bb, "node_start", Map.of("node", id, "autonomy", node.autonomy().name()));

        Gate.Result eg = node.entryGate().check(bb);
        bb.log(id, "entry_gate", eg.reason(), Map.of("passed", eg.passed()));
        if (!eg.passed()) {
            bb.setStatus(id, NodeStatus.SKIPPED);
            emit(bb, "node_skipped", Map.of("node", id, "reason", eg.reason()));
            if (node.critical()) {
                throw new SafeStopException("entry gate failed for critical node '" + id + "': " + eg.reason());
            }
            return;
        }

        enforcePolicy(bb, id, null, "pre");

        if ((node.autonomy() == Autonomy.NEEDS_APPROVAL || node.autonomy() == Autonomy.BLOCKED)
                && !bb.isApproved(id)) {
            metrics.recordApproval();
            bb.setStatus(id, NodeStatus.WAITING_APPROVAL);
            emit(bb, "await_approval", Map.of("node", id));
            boolean approved = node.autonomy() != Autonomy.BLOCKED && approval.approve(id, bb);
            bb.setApproval(id, approved);
            bb.log(id, "approval", "human checkpoint",
                    Map.of("approved", approved, "autonomy", node.autonomy().name()));
            if (!approved) {
                bb.setStatus(id, NodeStatus.SAFE_STOPPED);
                emit(bb, "approval_rejected", Map.of("node", id));
                throw new SafeStopException("human rejected high-impact node '" + id + "'");
            }
        }

        Blackboard.Snapshot snapshot = bb.snapshot();
        int attempt = 0;
        while (true) {
            attempt++;
            metrics.recordAttempt();
            Agent agent = node.agent();
            boolean usingFallback = false;
            if (attempt > 1 && node.fallback() != null && attempt == node.maxRetries() + 1) {
                agent = node.fallback();
                usingFallback = true;
                bb.log(id, "fallback", "switching to fallback agent");
            }
            try {
                agent.act(bb);
                Gate.Result xg = node.exitGate().check(bb);
                bb.log(id, "exit_gate", xg.reason(), Map.of("passed", xg.passed()));
                if (!xg.passed()) {
                    throw new RuntimeException("exit gate failed: " + xg.reason());
                }
                enforcePolicy(bb, id, latestArtifact(bb, id), "post");
                bb.setStatus(id, NodeStatus.SUCCEEDED);
                bb.setOutputHash(id, nodeSignature(bb, id));
                metrics.recordSuccess(id);
                emit(bb, "node_success", Map.of("node", id, "attempt", attempt, "fallback", usingFallback));
                return;
            } catch (SafeStopException sse) {
                throw sse;
            } catch (Exception exc) {
                metrics.recordFailure(id);
                bb.log(id, "error", "attempt " + attempt + " failed: " + exc.getMessage());
                emit(bb, "node_error", Map.of("node", id, "attempt", attempt, "error", String.valueOf(exc.getMessage())));
                if (attempt <= node.maxRetries()) {
                    metrics.recordRetry();
                    bb.restore(snapshot);
                    metrics.recordRollback();
                    bb.setStatus(id, NodeStatus.ROLLED_BACK);
                    bb.log(id, "rollback", "rolled back state; retrying (attempt '" + (attempt + 1) + "')");
                    emit(bb, "node_rollback", Map.of("node", id, "nextAttempt", attempt + 1));
                    continue;
                }

                bb.restore(snapshot);
                metrics.recordRollback();
                bb.setStatus(id, NodeStatus.FAILED);
                emit(bb, "node_failed", Map.of("node", id));
                if (node.critical()) {
                    throw new SafeStopException(
                            "critical node '" + id + "' failed after '" + attempt + "' attempts: " + exc.getMessage());
                }
                return;
            }
        }
    }

    public List<String> replanFrom(Blackboard bb, String changedNode) {
        Set<String> downstream = descendants(changedNode);
        for (String id : downstream) {
            bb.setStatus(id, NodeStatus.PENDING);
            bb.clearOutputHash(id);
        }
        metrics.recordReplan();
        List<String> invalidated = new ArrayList<>(downstream);
        invalidated.sort(String::compareTo);
        bb.log("orchestrator", "replan", "upstream '" + changedNode + "' changed; invalidated downstream",
                Map.of("invalidated", invalidated));
        emit(bb, "replan", Map.of("changed", changedNode, "invalidated", invalidated));
        return invalidated;
    }

    private Set<String> descendants(String id) {
        Set<String> result = new HashSet<>();
        List<String> frontier = new ArrayList<>(List.of(id));
        while (!frontier.isEmpty()) {
            String cur = frontier.remove(frontier.size() - 1);
            for (Node other : nodes.values()) {
                if (other.dependsOn().contains(cur) && result.add(other.id())) {
                    frontier.add(other.id());
                }
            }
        }
        return result;
    }

    // ---- helpers -----------------------------------------------------------
    private void enforcePolicy(Blackboard bb, String id, Artifact artifact, String phase) {
        for (PolicyResult r : policy.evaluate(id, artifact, bb)) {
            if (!r.allowed()) {
                bb.setStatus(id, NodeStatus.SAFE_STOPPED);
                emit(bb, "policy_block", Map.of("node", id, "reason", r.reason(), "phase", phase));
                throw new SafeStopException("policy blocked '" + id + "': " + r.reason());
            }
            if (r.requireApproval() && !bb.isApproved(id)) {
                metrics.recordApproval();
                boolean approved = approval.approve(id, bb);
                bb.setApproval(id, approved);
                bb.log(id, "approval", "policy-required approval (" + r.reason() + ")",
                        Map.of("approved", approved));
                if (!approved) {
                    bb.setStatus(id, NodeStatus.SAFE_STOPPED);
                    throw new SafeStopException("human rejected policy-gated node '" + id + "'");
                }
            }
        }
    }

    private Artifact latestArtifact(Blackboard bb, String id) {
        Artifact latest = null;
        for (Artifact a : bb.artifacts().values()) {
            if (a.producedBy().equals(id)) {
                latest = a;
            }
        }
        return latest;
    }

    private String nodeSignature(Blackboard bb, String id) {
        List<String> hashes = new ArrayList<>();
        for (Artifact a : bb.artifacts().values()) {
            if (a.producedBy().equals(id)) {
                hashes.add(a.hash());
            }
        }
        return Hashing.contentHash(hashes);
    }

    private void emit(Blackboard bb, String kind, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", bb.runId());
        payload.putAll(data);
        onEvent.accept(kind, payload);
    }
}
