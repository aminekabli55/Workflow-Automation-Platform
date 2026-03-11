package com.amine.geminiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;


import java.util.HashMap;

@Component
@Slf4j
public class GeminiApiClient {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${gemini.embedding-model:embedding-001}")
    private String embeddingModel;

    private final WebClient webClient;

    public GeminiApiClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public String generateText(String prompt) {
        try {
            log.info("Calling Gemini API with model: {}", model);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", prompt)));

            requestBody.put("contents", List.of(content));

            // Make API call
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(this::extractText)
                    .block();

            log.info("Successfully generated text");
            return response;

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            throw new RuntimeException("Failed to generate text: " + e.getMessage(), e);
        }
    }

    private String extractText(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                return "No response generated";
            }

            Map<String, Object> content =
                    (Map<String, Object>) candidates.get(0).get("content");

            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");

            if (parts == null || parts.isEmpty()) {
                return "No text in response";
            }

            return (String) parts.get(0).get("text");

        } catch (Exception e) {
            log.error("Error parsing response: {}", e.getMessage());
            return "Error parsing response";
        }
    }

    public String summarize(String text, String style) {
        String prompt = switch (style) {
            case "brief" -> "Summarize this in one sentence: " + text;
            case "detailed" -> "Provide a detailed summary of: " + text;
            case "bullet-points" -> "Summarize this as bullet points: " + text;
            default -> "Summarize: " + text;
        };

        return generateText(prompt);
    }
    public float[] generateEmbedding(String text) {
        try {
            Map<String, Object> requestBody = new HashMap<>();

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", text)));

            requestBody.put("content", content);

            Map<String, Object> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:embedContent")
                            .queryParam("key", apiKey)
                            .build(embeddingModel))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Map<String, Object> embedding =
                    (Map<String, Object>) response.get("embedding");

            List<Double> values =
                    (List<Double>) embedding.get("values");

            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i).floatValue();
            }

            return result;

        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
}