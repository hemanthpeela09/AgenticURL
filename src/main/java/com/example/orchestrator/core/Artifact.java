package com.example.orchestrator.core;

/** A typed, versioned, content-hashed output produced by a node/agent. */
public final class Artifact {

    public enum Kind { REQUIREMENT, DESIGN, CODE, TESTS, DOCS, REPORT }

    private final String name;
    private final Kind kind;
    private final String producedBy;
    private final Object content;
    private final int version;

    public Artifact(String name, Kind kind, String producedBy, Object content, int version) {
        this.name = name;
        this.kind = kind;
        this.producedBy = producedBy;
        this.content = content;
        this.version = version;
    }

    public Artifact(String name, Kind kind, String producedBy, Object content) {
        this(name, kind, producedBy, content, 1);
    }

    public Artifact withVersion(int newVersion) {
        return new Artifact(name, kind, producedBy, content, newVersion);
    }

    public String name() { return name; }
    public Kind kind() { return kind; }
    public String producedBy() { return producedBy; }
    public Object content() { return content; }
    public int version() { return version; }
    public String hash() { return Hashing.contentHash(content);}
}
