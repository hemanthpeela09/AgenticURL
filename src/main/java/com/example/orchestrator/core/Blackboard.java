package com.example.orchestrator.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared, persisted-ish run state: the single source of cross-stage context.
 * Holds artifacts, per-node status/output-hashes, open questions, approvals,
 * and an append-only lineage log for audit-grade traceability.
 */
public class Blackboard {

    private final String runId;
    private final String requirement;
    private final Map<String, Artifact> artifacts = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, NodeStatus> nodeStatus = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, String> nodeOutputHash = Collections.synchronizedMap(new LinkedHashMap<>());
    private final List<LineageEvent> lineage = Collections.synchronizedList(new ArrayList<>());
    private final List<String> openQuestions = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Boolean> approvals = Collections.synchronizedMap(new LinkedHashMap<>());
    private final AtomicLong seq = new AtomicLong(0);

    public Blackboard(String requirement) {
        this.runId = java.util.UUID.randomUUID().toString().substring(0, 12);
        this.requirement = requirement;
    }

    public String runId() { return runId; }
    public String requirement() { return requirement; }

    // ---- artifacts ----------------------------------------------------------
    public void put(Artifact artifact) {
        Artifact existing = artifacts.get(artifact.name());
        Artifact stored = existing == null ? artifact : artifact.withVersion(existing.version() + 1);
        artifacts.put(stored.name(), stored);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hash", stored.hash());
        data.put("kind", stored.kind().name());
        data.put("version", stored.version());
        log(artifact.producedBy(), "artifact",
                "wrote artifact '" + stored.name() + "' v" + stored.version(), data);
    }

    public Artifact get(String name) { return artifacts.get(name); }
    public boolean has(String name) { return artifacts.containsKey(name); }
    public Map<String, Artifact> artifacts() { return artifacts; }

    // ---- status -------------------------------------------------------------
    public NodeStatus status(String node) { return nodeStatus.getOrDefault(node, NodeStatus.PENDING); }
    public void setStatus(String node, NodeStatus status) { nodeStatus.put(node, status); }
    public Map<String, NodeStatus> statuses() { return nodeStatus; }

    public void setOutputHash(String node, String hash) { nodeOutputHash.put(node, hash); }
    public String outputHash(String node) { return nodeOutputHash.get(node); }
    public void clearOutputHash(String node) { nodeOutputHash.remove(node); }

    // ---- open questions / approvals ----------------------------------------
    public List<String> openQuestions() { return openQuestions; }
    public void addOpenQuestion(String q) { openQuestions.add(q); }
    public void clearOpenQuestions() { openQuestions.clear(); }

    public Map<String, Boolean> approvals() { return approvals; }
    public void setApproval(String node, boolean approved) { approvals.put(node, approved); }
    public boolean isApproved(String node) { return approvals.getOrDefault(node, false); }

    // ---- lineage / audit ----------------------------------------------------
    public void log(String node, String kind, String detail, Map<String, Object> data) {
        lineage.add(new LineageEvent(seq.incrementAndGet(), System.currentTimeMillis(),
                node, kind, detail, data == null ? Map.of() : data));
    }

    public void log(String node, String kind, String detail) { log(node, kind, detail, Map.of()); }
    public List<LineageEvent> lineage() { return lineage; }

    // ---- snapshot for rollback ---------------------------------------------
    public Snapshot snapshot() {
        return new Snapshot(new LinkedHashMap<>(artifacts), new LinkedHashMap<>(nodeStatus),
                new LinkedHashMap<>(nodeOutputHash), new ArrayList<>(openQuestions));
    }

    public void restore(Snapshot snap) {
        artifacts.clear();
        artifacts.putAll(snap.artifacts);
        nodeStatus.clear();
        nodeStatus.putAll(snap.nodeStatus);
        nodeOutputHash.clear();
        nodeOutputHash.putAll(snap.nodeOutputHash);
        openQuestions.clear();
        openQuestions.addAll(snap.openQuestions);
    }

    /** Immutable-ish snapshot of mutable state for rollback. */
    public static final class Snapshot {
        final Map<String, Artifact> artifacts;
        final Map<String, NodeStatus> nodeStatus;
        final Map<String, String> nodeOutputHash;
        final List<String> openQuestions;

        Snapshot(Map<String, Artifact> artifacts, Map<String, NodeStatus> nodeStatus,
                 Map<String, String> nodeOutputHash, List<String> openQuestions) {
            this.artifacts = artifacts;
            this.nodeStatus = nodeStatus;
            this.nodeOutputHash = nodeOutputHash;
            this.openQuestions = openQuestions;
        }
    }
}
