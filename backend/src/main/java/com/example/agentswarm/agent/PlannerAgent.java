package com.example.agentswarm.agent;

import com.example.agentswarm.dto.PlannerSpec;
import com.example.agentswarm.llm.LLMProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgent {

    private static final String SYSTEM_PROMPT = """
            You are the Planner Agent in an AI software engineering swarm.

            Convert the user's software requirement into a concise project specification.

            Create the specification specifically for the user's requirement.
            Do not copy or imitate an existing application.
            Do not include source code.
            Do not invent unnecessary features.

            The specification will be given to another AI agent that will
            independently implement the project.

            Respond with ONLY valid JSON matching this structure:

            {
              "projectName": "string",
              "requirements": ["string"],
              "entities": [
                {
                  "name": "string",
                  "fields": [
                    {
                      "name": "string",
                      "type": "string"
                    }
                  ]
                }
              ],
              "endpoints": [
                {
                  "method": "GET|POST|PUT|DELETE",
                  "path": "string",
                  "description": "string"
                }
              ],
              "database": {
                "engine": "postgresql",
                "tables": ["string"]
              },
              "testingRequirements": ["string"]
            }

            Do not return Markdown.
            Do not return explanations.
            Do not return source code.
            Return only the JSON object.
            """;

    private final LLMProvider llm;

    private final ObjectMapper mapper =
            new ObjectMapper();

    public PlannerAgent(LLMProvider llm) {
        this.llm = llm;
    }

    public PlannerSpec plan(String userRequirement) {

        System.out.println();
        System.out.println("========================================");
        System.out.println(">>> PLANNER AGENT -> GEMINI");
        System.out.println("========================================");

        String userPrompt = """
                Create a project specification for the following
                user requirement.

                USER REQUIREMENT:

                """ + userRequirement + """

                Design the specification specifically for this requirement.

                Include only functionality required by the user
                or directly necessary to implement it.

                Do not include source code.

                Return only the JSON object.
                """;

        String raw =
                llm.generate(
                        SYSTEM_PROMPT,
                        userPrompt
                );

        System.out.println();
        System.out.println("<<< PLANNER AGENT <- GEMINI");
        System.out.println("Planner response received.");
        System.out.println();

        System.out.println("Raw Planner response:");
        System.out.println("----------------------------------------");
        System.out.println(raw);
        System.out.println("----------------------------------------");

        String cleaned =
                stripFences(raw);

        try {

            PlannerSpec result =
                    mapper.readValue(
                            cleaned,
                            PlannerSpec.class
                    );

            validatePlannerSpec(result);

            System.out.println();
            System.out.println("Planner JSON parsed successfully.");

            return result;

        } catch (Exception e) {

            System.out.println();
            System.out.println("========================================");
            System.out.println("PLANNER JSON PARSING FAILED");
            System.out.println("========================================");

            System.out.println(
                    "Parsing error: "
                            + e.getMessage()
            );

            System.out.println();
            System.out.println("Cleaned response:");
            System.out.println("----------------------------------------");
            System.out.println(cleaned);
            System.out.println("----------------------------------------");

            e.printStackTrace();

            throw new RuntimeException(
                    "Planner returned invalid JSON: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private String stripFences(String response) {

        if (response == null) {
            return "";
        }

        String cleaned =
                response.trim();

        if (cleaned.startsWith("```json")) {

            cleaned =
                    cleaned.substring(
                            7
                    ).trim();

        } else if (cleaned.startsWith("```")) {

            cleaned =
                    cleaned.substring(
                            3
                    ).trim();
        }

        if (cleaned.endsWith("```")) {

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    ).trim();
        }

        return cleaned;
    }

    private void validatePlannerSpec(
            PlannerSpec spec
    ) {

        if (spec == null) {
            throw new IllegalArgumentException(
                    "Planner returned null specification."
            );
        }

        if (spec.projectName() == null
                || spec.projectName().isBlank()) {

            throw new IllegalArgumentException(
                    "Planner specification has no projectName."
            );
        }

        if (spec.requirements() == null) {

            throw new IllegalArgumentException(
                    "Planner specification has no requirements."
            );
        }

        if (spec.entities() == null) {

            throw new IllegalArgumentException(
                    "Planner specification has no entities."
            );
        }

        if (spec.endpoints() == null) {

            throw new IllegalArgumentException(
                    "Planner specification has no endpoints."
            );
        }

        if (spec.database() == null) {

            throw new IllegalArgumentException(
                    "Planner specification has no database information."
            );
        }

        if (spec.testingRequirements() == null) {

            throw new IllegalArgumentException(
                    "Planner specification has no testing requirements."
            );
        }
    }
}