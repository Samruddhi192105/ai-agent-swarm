package com.example.agentswarm.agent;

import com.example.agentswarm.dto.CoderOutput;
import com.example.agentswarm.dto.ReviewDecision;
import com.example.agentswarm.dto.TestResult;
import com.example.agentswarm.llm.LLMProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ReviewerAgent {

    private static final String SYSTEM_PROMPT = """
            You are the Reviewer Agent in an AI software engineering swarm.

            Evaluate the generated project against the original requirement
            and the supplied test results.

            Focus ONLY on:
            - correctness
            - requirement compliance
            - build status
            - test results

            Do not reproduce source code.
            Do not quote source code.
            Do not suggest copying existing projects.
            Do not request unrelated features.

            Decide whether the project should PASS or RETRY.

            If the project is acceptable:
            return PASS with an empty issues array.

            If something must be fixed:
            return RETRY with concise file-level issues.

            OUTPUT FORMAT:

            Return ONLY valid JSON:

            {
              "decision": "PASS",
              "issues": []
            }

            OR:

            {
              "decision": "RETRY",
              "issues": [
                {
                  "file": "file path",
                  "problem": "short problem description",
                  "suggestedFix": "short actionable fix"
                }
              ]
            }

            IMPORTANT:
            - decision must be exactly PASS or RETRY
            - issues must always be an array
            - file must be a string
            - problem must be a string
            - suggestedFix must be a string
            - Return JSON only
            - No Markdown
            - No code fences
            - No explanations
            """;

    private final LLMProvider llm;

    private final ObjectMapper mapper =
            new ObjectMapper();

    public ReviewerAgent(LLMProvider llm) {
        this.llm = llm;
    }

    // ============================================================
    // REVIEW
    // ============================================================

    public ReviewDecision review(
            String originalRequirement,
            CoderOutput project,
            TestResult testResult
    ) {

        System.out.println();
        System.out.println("========================================");
        System.out.println(">>> REVIEWER AGENT -> GEMINI");
        System.out.println("========================================");

        String prompt =
                buildPrompt(
                        originalRequirement,
                        project,
                        testResult
                );

        System.out.println(
                "Reviewer prompt length: "
                        + prompt.length()
        );

        String raw =
                llm.generate(
                        SYSTEM_PROMPT,
                        prompt
                );

        System.out.println();
        System.out.println("<<< REVIEWER AGENT <- GEMINI");
        System.out.println("Reviewer response received.");

        System.out.println();
        System.out.println("Raw reviewer response:");
        System.out.println(raw);

        try {

            ReviewDecision decision =
                    parseReviewDecision(raw);

            System.out.println();
            System.out.println(
                    "Reviewer decision: "
                            + decision.decision()
            );

            if (decision.issues() != null) {

                System.out.println(
                        "Reviewer issues: "
                                + decision.issues().size()
                );
            }

            return decision;

        } catch (Exception e) {

            System.out.println();
            System.out.println(
                    "========================================"
            );
            System.out.println(
                    "REVIEWER JSON PARSING FAILED"
            );
            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "Error: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Reviewer returned invalid JSON: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // ============================================================
    // PARSE REVIEWER RESPONSE
    // ============================================================

    private ReviewDecision parseReviewDecision(
            String raw
    ) {

        if (raw == null
                || raw.isBlank()) {

            throw new IllegalArgumentException(
                    "Reviewer returned an empty response."
            );
        }

        String cleaned =
                cleanJsonResponse(raw);

        // --------------------------------------------------------
        // FIRST ATTEMPT
        // --------------------------------------------------------

        try {

            ReviewDecision decision =
                    mapper.readValue(
                            cleaned,
                            ReviewDecision.class
                    );

            validateDecision(decision);

            return decision;

        } catch (Exception firstError) {

            System.out.println();
            System.out.println(
                    "Normal reviewer JSON parsing failed:"
            );

            System.out.println(
                    firstError.getMessage()
            );

            System.out.println(
                    "Attempting JSON formatting repair..."
            );

            // ----------------------------------------------------
            // SECOND ATTEMPT
            // ----------------------------------------------------

            try {

                String repaired =
                        repairJsonString(cleaned);

                ReviewDecision decision =
                        mapper.readValue(
                                repaired,
                                ReviewDecision.class
                        );

                validateDecision(decision);

                System.out.println(
                        "Reviewer JSON repaired successfully."
                );

                return decision;

            } catch (Exception secondError) {

                throw new IllegalArgumentException(
                        "Reviewer JSON could not be parsed. "
                                + "First error: "
                                + firstError.getMessage()
                                + ". Second error: "
                                + secondError.getMessage(),
                        secondError
                );
            }
        }
    }

    // ============================================================
    // CLEAN JSON RESPONSE
    // ============================================================

    private String cleanJsonResponse(
            String response
    ) {

        String cleaned =
                response.trim();

        // Remove ```json ... ```
        if (cleaned.startsWith("```json")) {

            cleaned =
                    cleaned.substring(7).trim();

            if (cleaned.endsWith("```")) {

                cleaned =
                        cleaned.substring(
                                0,
                                cleaned.length() - 3
                        ).trim();
            }

        }

        // Remove ``` ... ```
        else if (cleaned.startsWith("```")) {

            cleaned =
                    cleaned.substring(3).trim();

            if (cleaned.endsWith("```")) {

                cleaned =
                        cleaned.substring(
                                0,
                                cleaned.length() - 3
                        ).trim();
            }
        }

        /*
         * Sometimes an LLM adds text before or after JSON.
         *
         * Try to isolate the outer JSON object.
         */

        int firstBrace =
                cleaned.indexOf('{');

        int lastBrace =
                cleaned.lastIndexOf('}');

        if (firstBrace >= 0
                && lastBrace > firstBrace) {

            cleaned =
                    cleaned.substring(
                            firstBrace,
                            lastBrace + 1
                    ).trim();
        }

        return cleaned;
    }

    // ============================================================
    // REPAIR JSON STRING
    // ============================================================

    private String repairJsonString(
            String json
    ) {

        StringBuilder result =
                new StringBuilder();

        boolean insideString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {

            char c =
                    json.charAt(i);

            // ----------------------------------------------------
            // Previous character was escape character
            // ----------------------------------------------------

            if (escaped) {

                result.append(c);

                escaped = false;

                continue;
            }

            // ----------------------------------------------------
            // Escape character
            // ----------------------------------------------------

            if (c == '\\') {

                result.append(c);

                escaped = true;

                continue;
            }

            // ----------------------------------------------------
            // JSON string boundary
            // ----------------------------------------------------

            if (c == '"') {

                insideString =
                        !insideString;

                result.append(c);

                continue;
            }

            // ----------------------------------------------------
            // Raw newline inside JSON string
            // ----------------------------------------------------

            if (insideString
                    && c == '\n') {

                result.append("\\n");

                continue;
            }

            // ----------------------------------------------------
            // Raw carriage return
            // ----------------------------------------------------

            if (insideString
                    && c == '\r') {

                result.append("\\r");

                continue;
            }

            // ----------------------------------------------------
            // Raw tab
            // ----------------------------------------------------

            if (insideString
                    && c == '\t') {

                result.append("\\t");

                continue;
            }

            result.append(c);
        }

        return result.toString();
    }

    // ============================================================
    // VALIDATE REVIEW DECISION
    // ============================================================

    private void validateDecision(
            ReviewDecision decision
    ) {

        if (decision == null) {

            throw new IllegalArgumentException(
                    "Reviewer decision is null."
            );
        }

        if (decision.decision() == null
                || decision.decision().isBlank()) {

            throw new IllegalArgumentException(
                    "Reviewer decision is missing."
            );
        }

        String decisionValue =
                decision.decision()
                        .trim()
                        .toUpperCase();

        if (!decisionValue.equals("PASS")
                && !decisionValue.equals("RETRY")) {

            throw new IllegalArgumentException(
                    "Invalid reviewer decision: "
                            + decision.decision()
            );
        }

        if (decision.issues() == null) {

            throw new IllegalArgumentException(
                    "Reviewer issues array is missing."
            );
        }

        // --------------------------------------------------------
        // Validate individual issues
        // --------------------------------------------------------

        for (ReviewDecision.Issue issue :
                decision.issues()) {

            if (issue == null) {

                throw new IllegalArgumentException(
                        "Reviewer returned a null issue."
                );
            }

            if (issue.file() == null
                    || issue.file().isBlank()) {

                throw new IllegalArgumentException(
                        "Reviewer issue has no file."
                );
            }

            if (issue.problem() == null
                    || issue.problem().isBlank()) {

                throw new IllegalArgumentException(
                        "Reviewer issue has no problem description."
                );
            }

            if (issue.suggestedFix() == null
                    || issue.suggestedFix().isBlank()) {

                throw new IllegalArgumentException(
                        "Reviewer issue has no suggested fix."
                );
            }
        }

        // --------------------------------------------------------
        // PASS should normally have no issues
        // --------------------------------------------------------

        if (decisionValue.equals("PASS")
                && !decision.issues().isEmpty()) {

            System.out.println(
                    "Warning: Reviewer returned PASS with issues."
            );
        }
    }

    // ============================================================
    // BUILD REVIEW PROMPT
    // ============================================================

    private String buildPrompt(
            String requirement,
            CoderOutput project,
            TestResult testResult
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("""
                Review this generated software project.

                ORIGINAL REQUIREMENT:
                """);

        sb.append(requirement)
                .append("\n\n");

        // --------------------------------------------------------
        // TEST STATUS
        // --------------------------------------------------------

        sb.append("TEST STATUS: ")
                .append(testResult.status())
                .append("\n");

        sb.append("BUILD SUCCESSFUL: ")
                .append(testResult.buildSuccessful())
                .append("\n");

        sb.append("TESTS PASSED: ")
                .append(testResult.testsPassed())
                .append("\n");

        sb.append("TESTS FAILED: ")
                .append(testResult.testsFailed())
                .append("\n");

        sb.append("ERRORS: ")
                .append(testResult.errors())
                .append("\n\n");

        // --------------------------------------------------------
        // TEST LOG
        // --------------------------------------------------------

        sb.append("""
                TEST LOG:
                """);

        String log =
                testResult.rawLog();

        if (log != null) {

            /*
             * Keep the reviewer prompt reasonably small.
             */

            if (log.length() > 3000) {

                sb.append(
                        log.substring(0, 3000)
                );

            } else {

                sb.append(log);
            }
        }

        sb.append("\n\n");

        // --------------------------------------------------------
        // PROJECT FILES
        // --------------------------------------------------------

        sb.append(
                "PROJECT FILES:\n"
        );

        if (project != null
                && project.files() != null) {

            project.files().forEach(file -> {

                if (file != null) {

                    sb.append("- ")
                            .append(file.path())
                            .append("\n");
                }
            });
        }

        // --------------------------------------------------------
        // REVIEW INSTRUCTIONS
        // --------------------------------------------------------

        sb.append("""

                REVIEW:

                1. Check the original requirement.
                2. Check build status.
                3. Check test results.
                4. Identify only real problems.
                5. If acceptable, return PASS and [].
                6. If correction is required, return RETRY.
                7. For RETRY, provide concise file-level issues.
                8. Do not request unrelated functionality.
                9. Do not reproduce source code.

                Return ONLY valid JSON.
                """);

        return sb.toString();
    }
}