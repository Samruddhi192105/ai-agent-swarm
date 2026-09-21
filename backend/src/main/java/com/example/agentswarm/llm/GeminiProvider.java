package com.example.agentswarm.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GeminiProvider implements LLMProvider {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${llm.gemini.api-key:}")
    private String apiKey;

    @Value("${llm.gemini.model:gemini-3.6-flash}")
    private String model;

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private static final int MAX_RETRIES = 4;

    public GeminiProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {

        System.out.println();
        System.out.println("========================================");
        System.out.println(">>> GEMINI PROVIDER");
        System.out.println("========================================");
        System.out.println("Model: " + model);
        System.out.println("System prompt length: " + systemPrompt.length());
        System.out.println("User prompt length: " + userPrompt.length());

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is missing or empty."
            );
        }

        String combinedPrompt =
                systemPrompt
                        + "\n\n"
                        + userPrompt;

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "candidateCount", 1,
                "maxOutputTokens", 24000
                );

        Map<String, Object> body =
                Map.of(
                        "contents",
                        List.of(
                                Map.of(
                                        "parts",
                                        List.of(
                                                Map.of(
                                                        "text",
                                                        combinedPrompt
                                                )
                                        )
                                )
                        ),
                        "generationConfig",
                        generationConfig
                );

        String url = String.format(
                ENDPOINT,
                model
        );

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            try {

                System.out.println(
                        "Gemini request attempt "
                                + attempt
                                + "/"
                                + MAX_RETRIES
                );

                String response =
                        restClient.post()
                                .uri(url)
                                .header(
                                        "x-goog-api-key",
                                        apiKey
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(body)
                                .retrieve()
                                .body(String.class);

                if (response == null || response.isBlank()) {
                    throw new RuntimeException(
                            "Gemini returned an empty response."
                    );
                }

                System.out.println(
                        "Gemini request successful."
                );

                return extractText(response);

            } catch (RestClientResponseException e) {

                int status = e.getStatusCode().value();

                System.out.println(
                        "Gemini HTTP error: "
                                + status
                );

                System.out.println(
                        "Gemini error body: "
                                + e.getResponseBodyAsString()
                );

                if (!isRetryable(status)
                        || attempt == MAX_RETRIES) {

                    throw new RuntimeException(
                            "Gemini request failed with HTTP "
                                    + status
                                    + ": "
                                    + e.getResponseBodyAsString(),
                            e
                    );
                }

                long delay =
                        calculateBackoff(attempt);

                System.out.println(
                        "Temporary Gemini error."
                );

                System.out.println(
                        "Retrying in "
                                + delay
                                + " ms..."
                );

                sleep(delay);

            } catch (RuntimeException e) {

                /*
                 * Do not retry normal application/parsing errors.
                 * These are not temporary Gemini availability problems.
                 */
                System.out.println(
                        "Gemini request failed: "
                                + e.getMessage()
                );

                throw e;

            } catch (Exception e) {

                throw new RuntimeException(
                        "Unexpected error while calling Gemini.",
                        e
                );
            }
        }

        throw new RuntimeException(
                "Gemini request failed after all retry attempts."
        );
    }

    private boolean isRetryable(int status) {

        return status == 429
                || status == 500
                || status == 502
                || status == 503
                || status == 504;
    }

    private long calculateBackoff(int attempt) {

        long baseDelay;

        switch (attempt) {
            case 1 -> baseDelay = 2000;
            case 2 -> baseDelay = 4000;
            case 3 -> baseDelay = 8000;
            default -> baseDelay = 16000;
        }

        long jitter =
                ThreadLocalRandom.current()
                        .nextLong(0, 1000);

        return baseDelay + jitter;
    }

    private void sleep(long milliseconds) {

        try {

            Thread.sleep(milliseconds);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Gemini retry interrupted.",
                    e
            );
        }
    }

    private String extractText(String rawJson) {

        try {

            JsonNode root =
                    mapper.readTree(rawJson);

            JsonNode candidates =
                    root.path("candidates");

            if (!candidates.isArray()
                    || candidates.isEmpty()) {

                JsonNode error =
                        root.path("error");

                String status =
                        error.path("status")
                                .asText("");

                String message =
                        error.path("message")
                                .asText(
                                        "Gemini returned no candidates."
                                );

                throw new RuntimeException(
                        "Gemini error "
                                + status
                                + ": "
                                + message
                );
            }

            StringBuilder result =
                    new StringBuilder();

            for (JsonNode candidate : candidates) {

                String finishReason =
                        candidate.path("finishReason")
                                .asText("");

                if (!finishReason.isBlank()
                        && !"STOP".equalsIgnoreCase(finishReason)
                        && !"MAX_TOKENS".equalsIgnoreCase(finishReason)) {

                    throw new RuntimeException(
                            "Gemini generation stopped with finish reason: "
                                    + finishReason
                    );
                }

                JsonNode parts =
                        candidate
                                .path("content")
                                .path("parts");

                if (parts.isArray()) {

                    for (JsonNode part : parts) {

                        JsonNode text =
                                part.path("text");

                        if (!text.isMissingNode()
                                && !text.isNull()) {

                            result.append(
                                    text.asText()
                            );
                        }
                    }
                }
            }

            String output =
                    result.toString().trim();

            if (output.isEmpty()) {

                throw new RuntimeException(
                        "Gemini returned an empty text response."
                );
            }

            return output;

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse Gemini response.",
                    e
            );
        }
    }

    @Override
    public String name() {
        return "gemini";
    }
}