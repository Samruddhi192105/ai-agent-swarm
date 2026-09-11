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
            You are the Reviewer agent. You receive the original requirement, the generated
            project files, and the test results from the sandbox. Decide PASS or RETRY.
            If RETRY, give specific, actionable fixes tied to exact files - never vague comments
            like "code is wrong". Respond with ONLY valid JSON, no prose, no markdown fences:

            {
              "decision": "PASS|RETRY",
              "issues": [{"file": "string", "problem": "string", "suggestedFix": "string"}]
            }
            """;

    private final LLMProvider llm;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReviewerAgent(LLMProvider llm) {
        this.llm = llm;
    }

    public ReviewDecision review(String originalRequirement, CoderOutput project, TestResult testResult) {
        // Fast path: don't spend an LLM call reviewing something that already failed to build/test cleanly
        // when we can already tell the fix is "make the tests pass" - still ask the LLM for *how*.
        String prompt = buildPrompt(originalRequirement, project, testResult);
        String raw = llm.generate(SYSTEM_PROMPT, prompt);
        String cleaned = raw.replaceAll("(?s)```json|```", "").trim();
        try {
            return mapper.readValue(cleaned, ReviewDecision.class);
        } catch (Exception e) {
            throw new RuntimeException("Reviewer returned invalid JSON: " + cleaned, e);
        }
    }

    private String buildPrompt(String requirement, CoderOutput project, TestResult testResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("Original requirement: ").append(requirement).append("\n\n");
        sb.append("Test status: ").append(testResult.status()).append("\n");
        sb.append("Build successful: ").append(testResult.buildSuccessful()).append("\n");
        sb.append("Tests passed: ").append(testResult.testsPassed()).append("\n");
        sb.append("Tests failed: ").append(testResult.testsFailed()).append("\n");
        sb.append("Errors: ").append(testResult.errors()).append("\n\n");
        sb.append("Raw log (truncated):\n");
        String log = testResult.rawLog();
        sb.append(log.length() > 2000 ? log.substring(0, 2000) : log);
        sb.append("\n\nFiles in project: ");
        project.files().forEach(f -> sb.append(f.path()).append(", "));
        return sb.toString();
    }
}
