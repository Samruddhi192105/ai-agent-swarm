package com.example.agentswarm.agent;

import com.example.agentswarm.dto.CoderOutput;
import com.example.agentswarm.dto.PlannerSpec;
import com.example.agentswarm.dto.ReviewDecision;
import com.example.agentswarm.llm.LLMProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CoderAgent {

    private static final String SYSTEM_PROMPT = """
        You are the Coder agent in a multi-agent software engineering system.

        Generate the smallest complete and compilable Spring Boot Maven project
        that satisfies the provided project specification.

        OUTPUT RULES:
        - Return ONLY valid JSON.
        - No markdown.
        - No ``` fences.
        - Root object must contain ONLY "files".
        - Each file must contain only "path" and "content".
        - Do not include explanations.
        - Do not include unnecessary files.
        - Do not include unnecessary comments.
        - Do not duplicate functionality.
        - Do not include sample/demo code unless required.

        Generate original implementation code from scratch.
        Do not reproduce or imitate existing repositories or copyrighted code.
        Do not include URLs, citations, or references.

        Required files:
        - pom.xml
        - Dockerfile
        - main Spring Boot application class
        - required entities
        - required repositories
        - required controllers
        - services only when useful
        - DTOs only when useful
        - tests covering the main functionality

        Keep the implementation simple and minimal while fully satisfying
        the project requirements.

        Return exactly:
        {
          "files": [
            {
              "path": "pom.xml",
              "content": "..."
            }
          ]
        }
        """;

    private final LLMProvider llm;
    private final ObjectMapper mapper = new ObjectMapper();

    public CoderAgent(LLMProvider llm) {
        this.llm = llm;
    }

    public CoderOutput generate(PlannerSpec spec) {
        return callLLM(toPrompt(spec));
    }

    // Used on retries: same spec, plus the Reviewer's concrete fix instructions.
    public CoderOutput regenerate(PlannerSpec spec, ReviewDecision feedback) {
        StringBuilder prompt = new StringBuilder(toPrompt(spec));
        prompt.append("\n\nThe previous attempt failed review. Fix these issues:\n");
        for (ReviewDecision.Issue issue : feedback.issues()) {
            prompt.append("- ").append(issue.file()).append(": ").append(issue.problem())
                  .append(" -> ").append(issue.suggestedFix()).append("\n");
        }
        return callLLM(prompt.toString());
    }

    private CoderOutput callLLM(String prompt) {
    String raw = llm.generate(SYSTEM_PROMPT, prompt);

    String cleaned = raw
            .replaceAll("(?s)```json", "")
            .replaceAll("(?s)```", "")
            .trim();

    try {
        CoderOutput output = mapper.readValue(cleaned, CoderOutput.class);

        validateOutput(output);

        return output;

    } catch (Exception e) {
        throw new RuntimeException(
                "Coder returned invalid JSON: " + cleaned,
                e
        );
    }
}

private void validateOutput(CoderOutput output) {

    if (output == null || output.files() == null || output.files().isEmpty()) {
        throw new IllegalArgumentException("Coder generated no files.");
    }

    if (output.files().size() > 30) {
        throw new IllegalArgumentException(
                "Coder generated too many files. Maximum allowed is 30."
        );
    }

    boolean hasPom = false;
    boolean hasDockerfile = false;

    long totalSize = 0;

    for (CoderOutput.GeneratedFileDto file : output.files()) {

        if (file == null) {
            throw new IllegalArgumentException("Coder generated a null file.");
        }

        if (file.path() == null || file.path().isBlank()) {
            throw new IllegalArgumentException("Generated file has no path.");
        }

        if (file.content() == null) {
            throw new IllegalArgumentException(
                    "Generated file has no content: " + file.path()
            );
        }

        String path = file.path().replace("\\", "/");

        // Prevent path traversal
        if (path.startsWith("/")
                || path.matches("^[A-Za-z]:.*")
                || path.contains("../")
                || path.contains("..\\")) {

            throw new IllegalArgumentException(
                    "Unsafe generated file path: " + file.path()
            );
        }

        long fileSize = file.content().getBytes(
                java.nio.charset.StandardCharsets.UTF_8
        ).length;

        if (fileSize > 512 * 1024) {
            throw new IllegalArgumentException(
                    "Generated file is too large: " + file.path()
            );
        }

        totalSize += fileSize;

        if (path.equals("pom.xml")) {
            hasPom = true;
        }

        if (path.equals("Dockerfile")) {
            hasDockerfile = true;
        }
    }

    if (!hasPom) {
        throw new IllegalArgumentException(
                "Generated project must contain pom.xml."
        );
    }

    if (!hasDockerfile) {
        throw new IllegalArgumentException(
                "Generated project must contain Dockerfile."
        );
    }

    if (totalSize > 2 * 1024 * 1024) {
        throw new IllegalArgumentException(
                "Generated project is too large. Maximum is 2 MB."
        );
    }
}

    private String toPrompt(PlannerSpec spec) {
        try {
            return mapper.writeValueAsString(spec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
