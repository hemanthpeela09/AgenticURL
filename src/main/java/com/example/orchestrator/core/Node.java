package com.example.orchestrator.core;

import java.util.Collections;
import java.util.List;

/**
 * An SDLC stage in the dependency graph. Built via {@link Builder}.
 */
public final class Node {

    private final String id;
    private final Agent agent;
    private final List<String> dependsOn;
    private final Gate entryGate;
    private final Gate exitGate;
    private final Autonomy autonomy;
    private final int maxRetries;
    private final Agent fallback;
    private final String parallelGroup;
    private final boolean critical;

    private Node(Builder b) {
        this.id = b.id;
        this.agent = b.agent;
        this.dependsOn = List.copyOf(b.dependsOn);
        this.entryGate = b.entryGate;
        this.exitGate = b.exitGate;
        this.autonomy = b.autonomy;
        this.maxRetries = b.maxRetries;
        this.fallback = b.fallback;
        this.parallelGroup = b.parallelGroup;
        this.critical = b.critical;
    }

    public static Builder builder(String id, Agent agent) { return new Builder(id, agent); }

    public String id() { return id; }
    public Agent agent() { return agent; }
    public List<String> dependsOn() { return dependsOn; }
    public Gate entryGate() { return entryGate; }
    public Gate exitGate() { return exitGate; }
    public Autonomy autonomy() { return autonomy; }
    public int maxRetries() { return maxRetries; }
    public Agent fallback() { return fallback; }
    public String parallelGroup() { return parallelGroup; }
    public boolean critical() { return critical; }

    public static final class Builder {
        private final String id;
        private final Agent agent;
        private List<String> dependsOn = Collections.emptyList();
        private Gate entryGate = Gates.always();
        private Gate exitGate = Gates.always();
        private Autonomy autonomy = Autonomy.AUTO;
        private int maxRetries = 2;
        private Agent fallback;
        private String parallelGroup;
        private boolean critical = true;

        private Builder(String id, Agent agent) {
            this.id = id;
            this.agent = agent;
        }

        public Builder dependsOn(String... deps) {
            this.dependsOn = List.of(deps);
            return this;
        }

        public Builder entryGate(Gate g) {
            this.entryGate = g;
            return this;
        }

        public Builder exitGate(Gate g) {
            this.exitGate = g;
            return this;
        }

        public Builder autonomy(Autonomy a) {
            this.autonomy = a;
            return this;
        }

        public Builder maxRetries(int n) {
            this.maxRetries = n;
            return this;
        }

        public Builder fallback(Agent a) {
            this.fallback = a;
            return this;
        }

        public Builder parallelGroup(String g) {
            this.parallelGroup = g;
            return this;
        }

        public Builder critical(boolean c) {
            this.critical = c;
            return this;
        }

        public Node build() { return new Node(this); }
    }
}
