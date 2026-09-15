package com.example.agentswarm.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiProvider implements LLMProvider {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${llm.gemini.api-key:}")
    private String apiKey;

    @Value("${llm.gemini.model:gemini-3.6-flash}")
    private String model;

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public GeminiProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {

        System.out.println();
        System.out.println("========================================");
        System.out.println("GEMINI REQUEST");
        System.out.println("========================================");
        System.out.println("Model: " + model);
        System.out.println("System prompt length: "
                + (systemPrompt == null ? 0 : systemPrompt.length()));
        System.out.println("User prompt length: "
                + (userPrompt == null ? 0 : userPrompt.length()));
        System.out.println();
        System.out.println("USER PROMPT:");
        System.out.println("----------------------------------------");
        System.out.println(userPrompt);
        System.out.println("----------------------------------------");
        System.out.println();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key is missing. Set GEMINI_API_KEY in the environment."
            );
        }

        String combinedPrompt;

        if (systemPrompt == null || systemPrompt.isBlank()) {
            combinedPrompt = userPrompt;
        } else {
            combinedPrompt = systemPrompt + "\n\n" + userPrompt;
        }

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "candidateCount", 1,
                "maxOutputTokens", 24000,
                "thinkingConfig", Map.of(
                        "thinkingLevel", "minimal"
                )
        );

        Map<String, Object> body = Map.of(
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
                model,
                apiKey
        );

        try {

            String response = restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                throw new RuntimeException(
                        "Gemini returned an empty response."
                );
            }

            return extractText(response);

        } catch (RuntimeException e) {

            System.err.println();
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println("GEMINI REQUEST FAILED");
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println(e.getMessage());
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println();

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Gemini request failed: " + e.getMessage(),
                    e
            );
        }
    }

    private String extractText(String rawJson) {

        try {

            JsonNode root = mapper.readTree(rawJson);

            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {

                JsonNode error = root.path("error");

                if (!error.isMissingNode()) {

                    String message =
                            error.path("message").asText("");

                    String status =
                            error.path("status").asText("");

                    throw new RuntimeException(
                            "Gemini API error"
                                    + (status.isBlank()
                                    ? ""
                                    : " [" + status + "]")
                                    + ": "
                                    + (message.isBlank()
                                    ? rawJson
                                    : message)
                    );
                }

                throw new RuntimeException(
                        "Gemini returned no candidates: " + rawJson
                );
            }

            JsonNode candidate = candidates.get(0);

            String finishReason =
                    candidate
                            .path("finishReason")
                            .asText("");

            System.out.println(
                    "Gemini finish reason: " + finishReason
            );

            /*
             * Gemini RECITATION response.
             */
            if ("RECITATION".equalsIgnoreCase(finishReason)) {

                System.err.println();
                System.err.println(
                        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                );
                System.err.println(
                        "GEMINI RECITATION BLOCK"
                );
                System.err.println(
                        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                );
                System.err.println(
                        "Model: " + model
                );
                System.err.println(
                        "Finish reason: " + finishReason
                );
                System.err.println(
                        "Gemini blocked the generated content."
                );
                System.err.println(
                        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                );
                System.err.println();

                throw new RuntimeException(
                        "Gemini blocked the generated content because it "
                                + "resembled existing copyrighted material. "
                                + "The generation request must be rewritten "
                                + "to request an original implementation."
                );
            }

            /*
             * Other unsuccessful finish reasons.
             */
            if (!"STOP".equalsIgnoreCase(finishReason)) {

                String finishMessage =
                        candidate
                                .path("finishMessage")
                                .asText("");

                String message =
                        "Gemini did not return usable content. "
                                + "finishReason=" + finishReason;

                if (!finishMessage.isBlank()) {
                    message +=
                            ", finishMessage=" + finishMessage;
                }

                throw new RuntimeException(message);
            }

            JsonNode parts =
                    candidate
                            .path("content")
                            .path("parts");

            if (!parts.isArray() || parts.isEmpty()) {

                throw new RuntimeException(
                        "Gemini returned a successful response "
                                + "without any content parts."
                );
            }

            StringBuilder textBuilder =
                    new StringBuilder();

            for (JsonNode part : parts) {

                JsonNode textNode =
                        part.path("text");

                if (!textNode.isMissingNode()) {

                    String text =
                            textNode.asText("");

                    if (!text.isBlank()) {
                        textBuilder.append(text);
                    }
                }
            }

            String text =
                    textBuilder
                            .toString()
                            .trim();

            if (text.isBlank()) {

                throw new RuntimeException(
                        "Gemini returned empty generated content."
                );
            }

            return text;

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse Gemini response: "
                            + rawJson,
                    e
            );
        }
    }

    @Override
    public String name() {
        return "gemini";
    }
}