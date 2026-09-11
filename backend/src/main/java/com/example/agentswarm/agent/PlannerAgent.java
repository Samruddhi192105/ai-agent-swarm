package com.example.agentswarm.agent;

import com.example.agentswarm.dto.PlannerSpec;
import com.example.agentswarm.llm.LLMProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgent {

    private static final String SYSTEM_PROMPT = """
            You are the Planner agent in a multi-agent software engineering system.
            Given a one-line project requirement, produce a structured project specification.
            Respond with ONLY valid JSON matching this exact shape, no prose, no markdown fences:

            {
              "projectName": "string",
              "requirements": ["string"],
              "entities": [{"name": "string", "fields": [{"name": "string", "type": "string"}]}],
              "endpoints": [{"method": "GET|POST|PUT|DELETE", "path": "string", "description": "string"}],
              "database": {"engine": "postgresql", "tables": ["string"]},
              "testingRequirements": ["string"]
            }
            """;

    private final LLMProvider llm;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlannerAgent(LLMProvider llm) {
        this.llm = llm;
    }

    public PlannerSpec plan(String userRequirement) {
        String raw = llm.generate(SYSTEM_PROMPT, userRequirement);
        String cleaned = stripFences(raw);
        try {
            return mapper.readValue(cleaned, PlannerSpec.class);
        } catch (Exception e) {
            throw new RuntimeException("Planner returned invalid JSON: " + cleaned, e);
        }
    }

    private String stripFences(String s) {
        return s.replaceAll("(?s)```json|```", "").trim();
    }
}
