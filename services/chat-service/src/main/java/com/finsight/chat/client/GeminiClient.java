package com.finsight.chat.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Calls Google Gemini API to generate natural-language responses.
 * Uses the gemini-flash-latest model for low-latency conversational responses.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String MODEL = "gemini-flash-latest";

    @Value("${gemini.api-key:}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().baseUrl(GEMINI_BASE_URL).build();
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a natural-language response using Gemini.
     *
     * @param systemPrompt  The persona / instruction prompt for the AI
     * @param userMessage   The user's query
     * @param dataContext   Structured financial data as a formatted string
     * @return              Natural conversational response, or null if API unavailable
     */
    public String generate(String systemPrompt, String userMessage, String dataContext) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key not configured — falling back to template responses");
            return null;
        }

        // Build the combined prompt
        String fullPrompt = systemPrompt +
            "\n\n--- FINANCIAL DATA CONTEXT ---\n" + dataContext +
            "\n--- END OF DATA ---\n\n" +
            "User's question: " + userMessage;

        // Build Gemini request body
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", fullPrompt)))
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 512,
                "topP", 0.9
            )
        );

        try {
            Map<?, ?> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1beta/models/" + MODEL + ":generateContent")
                    .queryParam("key", apiKey)
                    .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response == null) return null;

            // Parse: response.candidates[0].content.parts[0].text
            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
            if (content == null) return null;

            List<?> parts = (List<?>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            Map<?, ?> part = (Map<?, ?>) parts.get(0);
            return (String) part.get("text");

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return null;
        }
    }
}
