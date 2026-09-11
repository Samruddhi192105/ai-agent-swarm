package com.example.agentswarm.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not set. Export it or set llm.gemini.api-key in application.yml");
        }

        String combinedPrompt =
        systemPrompt == null
                ? userPrompt
                : systemPrompt + "\n\n" + userPrompt;

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "candidateCount", 1,
                "maxOutputTokens", 12000,
                "thinkingConfig", Map.of(
                        "thinkingLevel", "minimal"
                )
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", combinedPrompt)
                                )
                        )
                ),
                "generationConfig", generationConfig
        );

        String url = String.format(ENDPOINT, model, apiKey);

        String response = restClient.post()
                .uri(url)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return extractText(response);
    }

    private String extractText(String rawJson) {
    try {
        JsonNode root = mapper.readTree(rawJson);

        JsonNode candidates = root.path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new RuntimeException(
                    "Gemini returned no candidates: " + rawJson
            );
        }

        JsonNode candidate = candidates.get(0);

        String finishReason = candidate.path("finishReason").asText("");

        if (!"STOP".equalsIgnoreCase(finishReason)) {
            String finishMessage = candidate.path("finishMessage").asText("");

            throw new RuntimeException(
                    "Gemini did not return usable content. " +
                    "finishReason=" + finishReason +
                    (finishMessage.isBlank() ? "" : ", message=" + finishMessage)
            );
        }

        JsonNode parts = candidate.path("content").path("parts");

        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException(
                    "Gemini returned empty content: " + rawJson
            );
        }

        String text = parts.get(0).path("text").asText("");

        if (text.isBlank()) {
            throw new RuntimeException(
                    "Gemini returned empty text: " + rawJson
            );
        }

        return text;

    } catch (RuntimeException e) {
        throw e;
    } catch (Exception e) {
        throw new RuntimeException(
                "Failed to parse Gemini response: " + rawJson, e
        );
    }
}

    @Override
    public String name() {
        return "gemini";
    }
}
