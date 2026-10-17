package com.quizly.app.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

@Service
public class GeminiTokenCounter implements AiTokenCounter {

    private final RestClient restClient;

    GeminiTokenCounter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Autowired
    public GeminiTokenCounter(
            @Value("${ai.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.timeout-seconds:45}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }

    @Override
    public int countTokens(String model, String systemInstruction, String userPrompt) {
        String modelName = model.startsWith("models/") ? model.substring("models/".length()) : model;
        String modelResource = "models/" + modelName;
        Map<String, Object> generateContentRequest = Map.of(
            "model", modelResource,
            "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
            "contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPrompt)))));
        Map<String, Object> requestBody = Map.of("generateContentRequest", generateContentRequest);

        JsonNode response = restClient.post()
            .uri("/v1beta/models/{model}:countTokens", modelName)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        int tokenCount = response == null ? -1 : response.path("totalTokens").asInt(-1);
        if (tokenCount < 0) {
            throw new IllegalStateException("La API de Gemini no devolvió totalTokens.");
        }
        return tokenCount;
    }
}