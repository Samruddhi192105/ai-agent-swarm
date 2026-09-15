package com.example.agentswarm.dto;

import java.util.List;

public record PlannerSpec(
        String projectName,
        List<String> requirements,
        List<Entity> entities,
        List<Endpoint> endpoints,
        Database database,
        List<String> testingRequirements
) {

    public record Entity(
            String name,
            List<Field> fields
    ) {}

    public record Field(
            String name,
            String type
    ) {}

    public record Endpoint(
            String method,
            String path,
            String description
    ) {}

    public record Database(
            String engine,
            List<String> tables
    ) {}
}