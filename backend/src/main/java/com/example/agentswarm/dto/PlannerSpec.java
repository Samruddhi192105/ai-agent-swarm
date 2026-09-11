package com.example.agentswarm.dto;

import java.util.List;
import java.util.Map;

// Structured output of the Planner agent. The Coder consumes this directly
// instead of parsing free-form text from the LLM.
public record PlannerSpec(
        String projectName,
        List<String> requirements,
        List<Entity> entities,
        List<Endpoint> endpoints,
        Map<String, Object> database,
        List<String> testingRequirements
) {
    public record Entity(String name, List<Field> fields) {}
    public record Field(String name, String type) {}
    public record Endpoint(String method, String path, String description) {}
}
