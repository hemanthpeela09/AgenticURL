package com.example.orchestrator.llm;

/* Pluggable LLM seam. Agents depend on this, never on a concrete model. */
public interface Llm {
    String name();

    String complete(String system, String prompt);
}
