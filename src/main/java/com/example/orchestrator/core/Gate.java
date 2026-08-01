package com.example.orchestrator.core;

import java.util.function.Predicate;

/**
 * A precondition (entry) or postcondition (exit) over the blackboard. Gates are
 * how the graph enforces preconditions and validates outputs rather than
 * blindly chaining tasks.
 */
public final class Gate {

    private final String name;
    private final String description;
    private final Predicate<Blackboard> predicate;

    public Gate(String name, String description, Predicate<Blackboard> predicate) {
        this.name = name;
        this.description = description;
        this.predicate = predicate;
    }

    public String name() { return name; }
    public String description() { return description; }

    public Result check(Blackboard bb) {
        try {
            boolean ok = predicate.test(bb);
            return new Result(ok, "gate '" + name + "' " + (ok ? "passed" : "failed")
                    + (description.isBlank() ? "" : ": " + description));
        } catch (RuntimeException ex) {
            return new Result(false, "gate '" + name + "' errored: " + ex.getMessage());
        }
    }

    public record Result(boolean passed, String reason) { }
}
