package com.example.orchestrator.core;

@FunctionalInterface
public interface Agent {
    void act(Blackboard bb) throws Exception;
}
