package com.example.orchestrator.scenario;

/** The three required scenarios (greenfield / brownfield / ambiguous). */
public enum Scenario {

    GREENFIELD("greenfield",
            "Build a URL shortener service with core APIs and click analytics."),
    BROWNFIELD("brownfield",
            "Add link expiry (TTL) and analytics to the existing URL shortener service."),
    AMBIGUOUS("ambiguous",
            "Make the links safer.");

    private final String key;
    private final String requirement;

    Scenario(String key, String requirement) {
        this.key = key;
        this.requirement = requirement;
    }

    public String key() { return key; }

    public String requirement() { return requirement; }

    public static Scenario fromKey(String key) {
        for (Scenario s : values()) {
            if (s.key.equalsIgnoreCase(key)) {
                return s;
            }
        }
        throw new IllegalArgumentException("unknown scenario: " + key
                + " (expected greenfield|brownfield|ambiguous)");
    }
}