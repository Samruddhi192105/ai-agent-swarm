package com.example.agentswarm.agent;

import com.example.agentswarm.dto.CoderOutput;
import com.example.agentswarm.dto.PlannerSpec;
import com.example.agentswarm.llm.LLMProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CoderAgent {

    /*
     * Increased from 12 to 16.
     *
     * This gives Gemini enough room to create one Java file
     * for each public class/interface instead of combining
     * multiple public types into invalid Java files.
     */
    private static final int MAX_FILES = 20;

    private static final int MAX_FILE_SIZE = 512 * 1024;

    private static final int MAX_TOTAL_SIZE = 1 * 1024 * 1024;

    private final LLMProvider llmProvider;

    private final ObjectMapper mapper = new ObjectMapper();

    public CoderAgent(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    // ============================================================
    // INITIAL GENERATION
    // ============================================================

    public CoderOutput generate(PlannerSpec spec) {

        System.out.println();
        System.out.println("========================================");
        System.out.println(">>> CODER AGENT -> GEMINI");
        System.out.println("========================================");

        String systemPrompt = buildSystemPrompt();

        String userPrompt = buildGenerationPrompt(spec);

        return callLLM(
                systemPrompt,
                userPrompt,
                spec
        );
    }

    // ============================================================
    // REGENERATION AFTER REVIEWER FEEDBACK
    // ============================================================

    public CoderOutput regenerate(
            PlannerSpec spec,
            String reviewerFeedback
    ) {

        System.out.println();
        System.out.println("========================================");
        System.out.println(">>> CODER AGENT REGENERATION -> GEMINI");
        System.out.println("========================================");

        String systemPrompt = buildSystemPrompt();

        String userPrompt =
                buildRegenerationPrompt(
                        spec,
                        reviewerFeedback
                );

        return callLLM(
                systemPrompt,
                userPrompt,
                spec
        );
    }

    // ============================================================
    // SYSTEM PROMPT
    // ============================================================

    private String buildSystemPrompt() {

        return """
                You are the Coder Agent in a software engineering swarm.

                Build a NEW implementation from the project specification.

                Write original project-specific code from first principles.
                Do not reproduce existing source code.

                TECHNOLOGY:
                - Java 21
                - Spring Boot
                - Maven
                - PostgreSQL when required
                - JUnit when testing is required
                - Docker
                - Docker Compose when PostgreSQL is required

                Keep the implementation small, clean, and functional.

                DOCKER REQUIREMENTS:

                - The generated application must be runnable inside Docker.
                - The Dockerfile must use a proper multi-stage Maven build.
                - Do not assume Maven is installed in the final runtime image.
                - Use a Maven builder image for compilation.
                - Use a lightweight Java 21 runtime image for the final application.
                - The final image must run the generated Spring Boot JAR.
                - Do not use ./mvnw unless Maven wrapper files are also generated.
                - When PostgreSQL is required, generate docker-compose.yml.
                - docker-compose.yml must contain both the application service and PostgreSQL service.
                - The application container must connect to PostgreSQL using the Docker Compose service name, never localhost.
                - Database configuration must support environment variables.
                - Do not hardcode database passwords or API keys.
                - Use sensible environment variable defaults where appropriate.

                Only implement requested functionality.
                Do not add unrelated features.

                Do not add unnecessary authentication.
                Do not add unnecessary JWT.
                Do not add unnecessary security configuration.
                Do not add unnecessary pagination.
                Do not add H2 unless explicitly required.
                Do not add unnecessary utility classes.
                Do not generate README files.
                Do not generate documentation files.

                FILE STRUCTURE RULES:

                - Maximum 20 files.
                - Every public Java class MUST be in its own .java file.
                - Every public Java interface MUST be in its own .java file.
                - Every public Java enum MUST be in its own .java file.
                - The Java filename MUST exactly match the public type name.
                - Never place multiple public Java classes in one file.
                - Never place multiple public Java interfaces in one file.
                - Never create files such as Entities.java or Repositories.java
                  containing multiple public Java types.
                - Keep package declarations consistent with file paths.
                - Use normal Java/Spring project structure.

                REQUIRED:
                - pom.xml
                - Dockerfile

                When PostgreSQL is required:
                - docker-compose.yml

                OUTPUT:

                Return ONLY one valid JSON object.

                {
                  "files": [
                    {
                      "path": "relative/path",
                      "content": "complete file content"
                    }
                  ]
                }

                JSON RULES:

                - No Markdown.
                - No code fences.
                - No explanations.
                - File content must be complete.
                - Escape quotes inside JSON strings.
                - Escape backslashes inside JSON strings.
                - Escape newline characters inside JSON strings.
                - Use safe relative paths only.
                - Never use absolute paths.
                - Never use ../.
                """;
    }

    // ============================================================
    // INITIAL GENERATION PROMPT
    // ============================================================

    private String buildGenerationPrompt(
            PlannerSpec spec
    ) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                Build this project.

                PROJECT:
                """);

        prompt.append(spec.projectName())
                .append("\n\n");

        // --------------------------------------------------------
        // REQUIREMENTS
        // --------------------------------------------------------

        prompt.append("REQUIREMENTS:\n");

        if (spec.requirements() != null) {

            for (String requirement :
                    spec.requirements()) {

                prompt.append("- ")
                        .append(requirement)
                        .append("\n");
            }
        }

        // --------------------------------------------------------
        // ENTITIES
        // --------------------------------------------------------

        prompt.append("\nENTITIES:\n");

        if (spec.entities() != null) {

            for (PlannerSpec.Entity entity :
                    spec.entities()) {

                prompt.append("- ")
                        .append(entity.name())
                        .append("\n");

                if (entity.fields() != null) {

                    for (PlannerSpec.Field field :
                            entity.fields()) {

                        prompt.append("  - ")
                                .append(field.name())
                                .append(": ")
                                .append(field.type())
                                .append("\n");
                    }
                }
            }
        }

        // --------------------------------------------------------
        // ENDPOINTS
        // --------------------------------------------------------

        prompt.append("\nENDPOINTS:\n");

        if (spec.endpoints() != null) {

            for (PlannerSpec.Endpoint endpoint :
                    spec.endpoints()) {

                prompt.append("- ")
                        .append(endpoint.method())
                        .append(" ")
                        .append(endpoint.path())
                        .append(" - ")
                        .append(endpoint.description())
                        .append("\n");
            }
        }

        // --------------------------------------------------------
        // DATABASE
        // --------------------------------------------------------

        prompt.append("\nDATABASE:\n");

        if (spec.database() != null) {

            prompt.append(
                    spec.database().toString()
            );
        }

        // --------------------------------------------------------
        // TESTING
        // --------------------------------------------------------

        prompt.append("\n\nTESTING:\n");

        if (spec.testingRequirements() != null) {

            for (String requirement :
                    spec.testingRequirements()) {

                prompt.append("- ")
                        .append(requirement)
                        .append("\n");
            }
        }

        // --------------------------------------------------------
        // FINAL INSTRUCTIONS
        // --------------------------------------------------------

        prompt.append("""

                IMPLEMENTATION RULES:

                - Maximum 16 files.
                - Include pom.xml.
                - Include Dockerfile.
                - If PostgreSQL is required, include docker-compose.yml.
                - Implement only requested functionality.

                DOCKER IMPLEMENTATION:

                - Dockerfile must build the Spring Boot application using Maven.
                - Use a Maven builder stage.
                - Use Java 21 runtime for the final image.
                - The final image must run the generated JAR.
                - Do not depend on Maven being installed in the runtime image.
                - Do not use ./mvnw unless Maven wrapper files are included.
                - If PostgreSQL is required, docker-compose.yml must define:
                  1. application service
                  2. PostgreSQL service
                - The application service must depend on PostgreSQL.
                - The application must connect to PostgreSQL using the PostgreSQL service name.
                - Never use localhost for PostgreSQL communication between containers.
                - Database URL, username, and password must be configurable using environment variables.
                - Never hardcode secrets.
                - Keep Java classes concise.
                - Keep tests concise.
                - No README.
                - No documentation.
                - No unnecessary files.

                DOCKERFILE EXPECTATION:

                Use a structure similar to:

                FROM maven:3.9-eclipse-temurin-21 AS build
                WORKDIR /app
                COPY pom.xml .
                COPY src ./src
                RUN mvn -B clean package -DskipTests

                FROM eclipse-temurin:21-jre
                WORKDIR /app
                COPY --from=build /app/target/*.jar app.jar
                EXPOSE 8080
                ENTRYPOINT ["java", "-jar", "app.jar"]

                Do not copy this example literally if the project requires different configuration.
                Adapt it to the generated project.

                DOCKER COMPOSE EXPECTATION FOR POSTGRESQL:

                services:
                app:
                build: .
                depends_on:
                - postgres
                environment:
                DB_HOST: postgres
                DB_PORT: 5432
                DB_NAME: <database>
                DB_USER: <username>
                DB_PASSWORD: <password>

                postgres:
                image: postgres:16
                environment:
                POSTGRES_DB: <database>
                POSTGRES_USER: <username>
                POSTGRES_PASSWORD: <password>

                JAVA FILE STRUCTURE:

                Every public Java type must have its own file.

                For example:

                src/main/java/com/example/app/model/Book.java
                src/main/java/com/example/app/model/Member.java
                src/main/java/com/example/app/model/BorrowRecord.java

                src/main/java/com/example/app/repository/BookRepository.java
                src/main/java/com/example/app/repository/MemberRepository.java
                src/main/java/com/example/app/repository/BorrowRecordRepository.java

                DO NOT create:

                Entities.java
                Repositories.java
                OtherRepositories.java

                if those files contain multiple public Java types.

                The filename must match the public class/interface name.

                Return ONLY the JSON object.
                """);

        return prompt.toString();
    }

    // ============================================================
    // REGENERATION PROMPT
    // ============================================================

    private String buildRegenerationPrompt(
            PlannerSpec spec,
            String reviewerFeedback
    ) {

        StringBuilder prompt =
                new StringBuilder(
                        buildGenerationPrompt(spec)
                );

        prompt.append(
                "\n\nREVIEWER FEEDBACK:\n"
        );

        if (reviewerFeedback != null
                && !reviewerFeedback.isBlank()) {

            prompt.append(
                    reviewerFeedback
            );

        } else {

            prompt.append(
                    "No detailed feedback was supplied."
            );
        }

        prompt.append("""

                REGENERATION:

                Create a fresh corrected implementation.

                Fix every problem identified by the reviewer.

                IMPORTANT:

                Follow the Java file structure rules exactly.

                Every public Java class must be in its own
                correctly named .java file.

                Every public Java interface must be in its own
                correctly named .java file.

                Do not combine multiple public Java types
                into Entities.java, Repositories.java,
                OtherRepositories.java, or similar files.

                Keep all original requirements.

                Do not add unrelated functionality.

                Maximum 16 files.

                Return ONLY the JSON object.
                """);

        return prompt.toString();
    }

    // ============================================================
    // CALL GEMINI
    // ============================================================

    private CoderOutput callLLM(
            String systemPrompt,
            String userPrompt,
            PlannerSpec spec
    ) {

        System.out.println(
                "Coder system prompt length: "
                        + systemPrompt.length()
        );

        System.out.println(
                "Coder user prompt length: "
                        + userPrompt.length()
        );

        String rawResponse =
                llmProvider.generate(
                        systemPrompt,
                        userPrompt
                );

        try {

            return parseCoderOutput(
                    rawResponse
            );

        } catch (Exception firstError) {

            System.out.println();
            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "CODER JSON PARSING FAILED"
            );

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "First parsing error:"
            );

            System.out.println(
                    firstError.getMessage()
            );

            System.out.println();

            System.out.println(
                    "Attempting one fresh Coder generation..."
            );

            /*
             * IMPORTANT:
             *
             * Do not send the previous generated source
             * back to Gemini.
             *
             * Gemini receives only the Planner specification.
             */

            String retryPrompt =
                    buildFreshRetryPrompt(spec);

            String retryResponse =
                    llmProvider.generate(
                            systemPrompt,
                            retryPrompt
                    );

            try {

                return parseCoderOutput(
                        retryResponse
                );

            } catch (Exception secondError) {

                throw new RuntimeException(
                        "Coder Agent returned invalid JSON after retry. "
                                + "First parsing error: "
                                + firstError.getMessage()
                                + ". Second parsing error: "
                                + secondError.getMessage(),
                        secondError
                );
            }
        }
    }

    // ============================================================
    // FRESH RETRY PROMPT
    // ============================================================

    private String buildFreshRetryPrompt(
            PlannerSpec spec
    ) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                Generate a fresh implementation.

                PROJECT:
                """);

        prompt.append(spec.projectName())
                .append("\n\n");

        // --------------------------------------------------------
        // REQUIREMENTS
        // --------------------------------------------------------

        prompt.append("REQUIREMENTS:\n");

        if (spec.requirements() != null) {

            for (String requirement :
                    spec.requirements()) {

                prompt.append("- ")
                        .append(requirement)
                        .append("\n");
            }
        }

        // --------------------------------------------------------
        // ENTITIES
        // --------------------------------------------------------

        prompt.append("\nENTITIES:\n");

        if (spec.entities() != null) {

            for (PlannerSpec.Entity entity :
                    spec.entities()) {

                prompt.append("- ")
                        .append(entity.name())
                        .append("\n");

                if (entity.fields() != null) {

                    for (PlannerSpec.Field field :
                            entity.fields()) {

                        prompt.append("  - ")
                                .append(field.name())
                                .append(": ")
                                .append(field.type())
                                .append("\n");
                    }
                }
            }
        }

        // --------------------------------------------------------
        // ENDPOINTS
        // --------------------------------------------------------

        prompt.append("\nENDPOINTS:\n");

        if (spec.endpoints() != null) {

            for (PlannerSpec.Endpoint endpoint :
                    spec.endpoints()) {

                prompt.append("- ")
                        .append(endpoint.method())
                        .append(" ")
                        .append(endpoint.path())
                        .append(" - ")
                        .append(endpoint.description())
                        .append("\n");
            }
        }

        // --------------------------------------------------------
        // DATABASE
        // --------------------------------------------------------

        prompt.append("\nDATABASE:\n");

        if (spec.database() != null) {

            prompt.append(
                    spec.database().toString()
            );
        }

        // --------------------------------------------------------
        // TESTING
        // --------------------------------------------------------

        prompt.append("\n\nTESTING:\n");

        if (spec.testingRequirements() != null) {

            for (String requirement :
                    spec.testingRequirements()) {

                prompt.append("- ")
                        .append(requirement)
                        .append("\n");
            }
        }

        // --------------------------------------------------------
        // OUTPUT
        // --------------------------------------------------------

        prompt.append("""

                RULES:

                - Maximum 16 files.
                - Include pom.xml.
                - Include Dockerfile.
                - Implement only required functionality.
                - Keep source files concise.
                - Keep tests concise.
                - No README.
                - No documentation.
                - No unnecessary files.
                - No absolute paths.
                - No ../ paths.

                JAVA FILE STRUCTURE:

                Every public Java class must be in its own
                correctly named .java file.

                Every public Java interface must be in its own
                correctly named .java file.

                Every public Java enum must be in its own
                correctly named .java file.

                Never put multiple public Java types in one file.

                Do not create files such as:

                Entities.java
                Repositories.java
                OtherRepositories.java

                when they contain multiple public Java types.

                IMPORTANT JSON RULE:

                File contents are JSON strings.
                Escape every newline as \\n.
                Escape every quote inside file content.
                Escape backslashes correctly.

                Return ONLY valid JSON:

                {
                  "files": [
                    {
                      "path": "relative/path",
                      "content": "complete file content"
                    }
                  ]
                }

                No Markdown.
                No code fences.
                No explanations.
                """);

        return prompt.toString();
    }

    // ============================================================
    // PARSE CODER OUTPUT
    // ============================================================

    private CoderOutput parseCoderOutput(
            String rawResponse
    ) {

        if (rawResponse == null
                || rawResponse.isBlank()) {

            throw new IllegalArgumentException(
                    "Coder Agent returned an empty response."
            );
        }

        System.out.println();

        System.out.println(
                "Coder response length: "
                        + rawResponse.length()
        );

        String cleaned =
                cleanJsonResponse(rawResponse);

        // --------------------------------------------------------
        // FIRST ATTEMPT: NORMAL JSON
        // --------------------------------------------------------

        try {

            CoderOutput output =
                    mapper.readValue(
                            cleaned,
                            CoderOutput.class
                    );

            /*
             * Remove duplicate paths before validation.
             *
             * If Gemini returns the same path twice,
             * the latest version is retained.
             */

            output =
                    removeDuplicateFiles(output);

            validateOutput(output);

            System.out.println(
                    "Coder JSON parsed successfully."
            );

            System.out.println(
                    "Generated files: "
                            + output.files().size()
            );

            return output;

        } catch (Exception firstError) {

            System.out.println();
            System.out.println(
                    "Normal JSON parsing failed."
            );

            System.out.println(
                    firstError.getMessage()
            );

            System.out.println(
                    "Attempting JSON formatting repair..."
            );

            // ----------------------------------------------------
            // SECOND ATTEMPT: REPAIR RAW NEWLINES/TABS
            // ----------------------------------------------------

            try {

                String repaired =
                        repairJsonString(cleaned);

                CoderOutput output =
                        mapper.readValue(
                                repaired,
                                CoderOutput.class
                        );

                output =
                        removeDuplicateFiles(output);

                validateOutput(output);

                System.out.println();

                System.out.println(
                        "Coder JSON repaired successfully."
                );

                System.out.println(
                        "Generated files: "
                                + output.files().size()
                );

                return output;

            } catch (Exception secondError) {

                System.out.println();
                System.out.println(
                        "JSON repair failed."
                );

                System.out.println(
                        secondError.getMessage()
                );

                throw new IllegalArgumentException(
                        "Failed to parse Coder Agent output: "
                                + secondError.getMessage(),
                        secondError
                );
            }
        }
    }

    // ============================================================
    // REPAIR JSON STRINGS
    // ============================================================

    private String repairJsonString(
            String json
    ) {

        StringBuilder result =
                new StringBuilder();

        boolean insideString = false;

        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {

            char c = json.charAt(i);

            // ----------------------------------------------------
            // Previous character was escape character
            // ----------------------------------------------------

            if (escaped) {

                result.append(c);

                escaped = false;

                continue;
            }

            // ----------------------------------------------------
            // Detect escape character
            // ----------------------------------------------------

            if (c == '\\') {

                result.append(c);

                escaped = true;

                continue;
            }

            // ----------------------------------------------------
            // Detect JSON string boundaries
            // ----------------------------------------------------

            if (c == '"') {

                insideString = !insideString;

                result.append(c);

                continue;
            }

            // ----------------------------------------------------
            // Repair raw newline
            // ----------------------------------------------------

            if (insideString && c == '\n') {

                result.append("\\n");

                continue;
            }

            // ----------------------------------------------------
            // Repair raw carriage return
            // ----------------------------------------------------

            if (insideString && c == '\r') {

                result.append("\\r");

                continue;
            }

            // ----------------------------------------------------
            // Repair raw tab
            // ----------------------------------------------------

            if (insideString && c == '\t') {

                result.append("\\t");

                continue;
            }

            result.append(c);
        }

        return result.toString();
    }

    // ============================================================
    // REMOVE DUPLICATE FILES
    // ============================================================

    private CoderOutput removeDuplicateFiles(
            CoderOutput output
    ) {

        if (output == null
                || output.files() == null) {

            return output;
        }

        Map<String, CoderOutput.GeneratedFileDto>
                uniqueFiles =
                new LinkedHashMap<>();

        for (CoderOutput.GeneratedFileDto file :
                output.files()) {

            if (file == null) {
                continue;
            }

            String path =
                    file.path();

            if (path == null
                    || path.isBlank()) {

                continue;
            }

            /*
             * If Gemini returns the same path twice,
             * the latest version replaces the previous one.
             */

            uniqueFiles.put(
                    path,
                    file
            );
        }

        return new CoderOutput(
                new ArrayList<>(
                        uniqueFiles.values()
                )
        );
    }

    // ============================================================
    // CLEAN MARKDOWN CODE FENCES
    // ============================================================

    private String cleanJsonResponse(
            String response
    ) {

        String cleaned =
                response.trim();

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

        } else if (cleaned.startsWith("```")) {

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

        return cleaned;
    }

    // ============================================================
    // VALIDATE GENERATED PROJECT
    // ============================================================

    private void validateOutput(
            CoderOutput output
    ) {

        if (output == null) {

            throw new IllegalArgumentException(
                    "Coder output is null."
            );
        }

        if (output.files() == null
                || output.files().isEmpty()) {

            throw new IllegalArgumentException(
                    "Coder output contains no files."
            );
        }

        // --------------------------------------------------------
        // MAX FILE COUNT
        // --------------------------------------------------------

        if (output.files().size() > MAX_FILES) {

            throw new IllegalArgumentException(
                    "Coder generated too many files. "
                            + "Maximum allowed: "
                            + MAX_FILES
            );
        }

        Set<String> paths =
                new HashSet<>();

        int totalSize = 0;

        boolean hasPom = false;

        boolean hasDockerfile = false;

        for (CoderOutput.GeneratedFileDto file :
                output.files()) {

            if (file == null) {

                throw new IllegalArgumentException(
                        "Coder output contains a null file."
                );
            }

            String path =
                    file.path();

            String content =
                    file.content();

            // ----------------------------------------------------
            // PATH VALIDATION
            // ----------------------------------------------------

            if (path == null
                    || path.isBlank()) {

                throw new IllegalArgumentException(
                        "Generated file has an empty path."
                );
            }

            if (path.startsWith("/")
                    || path.startsWith("\\")
                    || path.contains("../")
                    || path.contains("..\\")
                    || path.matches("^[A-Za-z]:.*")) {

                throw new IllegalArgumentException(
                        "Unsafe generated file path: "
                                + path
                );
            }

            // ----------------------------------------------------
            // DUPLICATE FILE CHECK
            // ----------------------------------------------------

            if (!paths.add(path)) {

                throw new IllegalArgumentException(
                        "Duplicate generated file path: "
                                + path
                );
            }

            // ----------------------------------------------------
            // CONTENT VALIDATION
            // ----------------------------------------------------

            if (content == null) {

                throw new IllegalArgumentException(
                        "Generated file has null content: "
                                + path
                );
            }

            int fileSize =
                    content.getBytes(
                            StandardCharsets.UTF_8
                    ).length;

            if (fileSize > MAX_FILE_SIZE) {

                throw new IllegalArgumentException(
                        "Generated file is too large: "
                                + path
                );
            }

            totalSize += fileSize;

            if (totalSize > MAX_TOTAL_SIZE) {

                throw new IllegalArgumentException(
                        "Total generated project size exceeds "
                                + MAX_TOTAL_SIZE
                                + " bytes."
                );
            }

            // ----------------------------------------------------
            // REQUIRED FILES
            // ----------------------------------------------------

            if ("pom.xml".equals(path)) {

                hasPom = true;
            }

            if ("Dockerfile".equalsIgnoreCase(path)) {

                hasDockerfile = true;
            }
        }

        // --------------------------------------------------------
        // POM REQUIRED
        // --------------------------------------------------------

        if (!hasPom) {

            throw new IllegalArgumentException(
                    "Generated project must contain pom.xml."
            );
        }

        // --------------------------------------------------------
        // DOCKERFILE REQUIRED
        // --------------------------------------------------------

        if (!hasDockerfile) {

            throw new IllegalArgumentException(
                    "Generated project must contain a Dockerfile."
            );
        }
    }
}