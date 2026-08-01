package com.example.orchestrator.core;

/** Factory methods for common gates. */
public final class Gates {

    private Gates() { }

    public static Gate always() {
        return new Gate("always", "no precondition", bb -> true);
    }

    public static Gate requires(String... names) {
        return new Gate("requires:" + String.join(",", names),
                "requires artifacts " + java.util.Arrays.toString(names),
                bb -> java.util.Arrays.stream(names).allMatch(bb::has));
    }

    public static Gate produces(String name) {
        return new Gate("produces:" + name,
                "must produce artifact '" + name + "'",
                bb -> bb.has(name));
    }

    public static Gate noOpenQuestions() {
        return new Gate("no_open_questions", "all requirement ambiguities resolved",
                bb -> bb.openQuestions().isEmpty());
    }

    /** Combine gates with logical AND, reporting the first failing sub-gate. */
    public static Gate all(String name, Gate... gates) {
        return new Gate(name, "all of " + gates.length + " conditions", bb -> {
            for (Gate g : gates) {
                if (!g.check(bb).passed()) {
                    return false;
                }
            }
            return true;
        });
    }
}
