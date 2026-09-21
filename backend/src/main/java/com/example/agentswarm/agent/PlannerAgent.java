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

            IMPORTANT JSON RULES:

            - Return ONLY the JSON object.
            - Do not return Markdown.
            - Do not use ```json.
            - Do not use ``` fences.
            - Do not return explanations.
            - Do not return comments.
            - Use double quotes for all JSON keys and string values.
            - Separate every JSON property with a comma.
            - Do not add trailing commas.
            - Ensure all { } and [ ] brackets are properly closed.
            - Ensure the response can be parsed directly by Jackson ObjectMapper.

            """;

    private final LLMProvider llm;

    private final ObjectMapper mapper = new ObjectMapper();

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

        String raw = llm.generate(
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

        // First attempt
        try {
            return parsePlannerResponse(raw);
        } catch (Exception firstException) {

            System.out.println();
            System.out.println("========================================");
            System.out.println("PLANNER JSON INVALID");
            System.out.println("========================================");
            System.out.println(
                    "First parsing error: "
                            + firstException.getMessage()
            );

            System.out.println();
            System.out.println("Attempting Planner JSON repair with Gemini...");

            // Second Gemini call specifically for repairing JSON
            String repairPrompt = """
                    The following response from a Planner Agent is supposed
                    to be a JSON object matching the PlannerSpec structure.

                    It is INVALID JSON.

                    Fix the JSON syntax while preserving the information.

                    IMPORTANT:
                    - Return ONLY valid JSON.
                    - Do not return Markdown.
                    - Do not use ```json.
                    - Do not use ``` fences.
                    - Do not add explanations.
                    - Do not add comments.
                    - Do not invent new requirements.
                    - Do not remove useful information.
                    - Use double quotes for JSON keys and string values.
                    - Make sure commas are present between properties.
                    - Make sure all brackets are correctly closed.
                    - Make sure the result can be parsed by Jackson ObjectMapper.

                    EXPECTED STRUCTURE:

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

                    INVALID PLANNER RESPONSE:

                    %s

                    Return ONLY the corrected JSON object.
                    """.formatted(raw);

            String repaired = llm.generate(
                    SYSTEM_PROMPT,
                    repairPrompt
            );

            System.out.println();
            System.out.println("Planner repair response:");
            System.out.println("----------------------------------------");
            System.out.println(repaired);
            System.out.println("----------------------------------------");

            try {

                PlannerSpec result = parsePlannerResponse(repaired);

                System.out.println();
                System.out.println("========================================");
                System.out.println("PLANNER JSON REPAIR SUCCESSFUL");
                System.out.println("========================================");

                return result;

            } catch (Exception secondException) {

                System.out.println();
                System.out.println("========================================");
                System.out.println("PLANNER JSON REPAIR FAILED");
                System.out.println("========================================");

                System.out.println(
                        "Repair parsing error: "
                                + secondException.getMessage()
                );

                throw new RuntimeException(
                        "Planner returned invalid JSON even after repair: "
                                + secondException.getMessage(),
                        secondException
                );
            }
        }
    }

    private PlannerSpec parsePlannerResponse(String response)
            throws Exception {

        String cleaned = stripFences(response);

        PlannerSpec result = mapper.readValue(
                cleaned,
                PlannerSpec.class
        );

        validatePlannerSpec(result);

        System.out.println();
        System.out.println("Planner JSON parsed successfully.");

        return result;
    }

    private String stripFences(String response) {

        if (response == null) {
            return "";
        }

        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(
                    0,
                    cleaned.length() - 3
            ).trim();
        }

        return cleaned;
    }

    private void validatePlannerSpec(PlannerSpec spec) {

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