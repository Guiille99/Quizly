package com.quizly.app.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.quizly.app.exception.AiGenerationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GeminiApiClient {

    private final RestClient restClient;
    private final String model;
    private final AiTokenCounter tokenCounter;
    private final int retryMaxAttempts;
    private final long retryInitialBackoffMs;

    @Autowired
    public GeminiApiClient(
            @Value("${ai.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:gemini-1.5-flash}") String model,
            @Value("${ai.timeout-seconds:45}") int timeoutSeconds,
            @Value("${ai.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${ai.retry.initial-backoff-ms:500}") long retryInitialBackoffMs,
            AiTokenCounter tokenCounter) {
            this(model, tokenCounter, retryMaxAttempts, retryInitialBackoffMs,
                createRestClient(baseUrl, apiKey, timeoutSeconds));
    }

    GeminiApiClient(
            String model,
                AiTokenCounter tokenCounter,
            int retryMaxAttempts,
            long retryInitialBackoffMs,
            RestClient restClient) {
        this.restClient = restClient;
        this.model = model;
        this.tokenCounter = tokenCounter;
        this.retryMaxAttempts = Math.max(1, retryMaxAttempts);
        this.retryInitialBackoffMs = Math.max(0, retryInitialBackoffMs);
    }

    GeminiApiClient(
            RestClient restClient,
            String model,
            AiTokenCounter tokenCounter,
            int retryMaxAttempts,
            long retryInitialBackoffMs) {
        this(model, tokenCounter, retryMaxAttempts, retryInitialBackoffMs, restClient);
    }

    public int countTokens(String systemInstruction, String userPrompt) {
        return with503Retries(() -> tokenCounter.countTokens(model, systemInstruction, userPrompt));
    }

    public String generate(String systemInstruction, String userPrompt, int maxOutputTokens) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemInstruction),
                        Map.of("role", "user", "content", userPrompt)),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.3,
                "max_tokens", maxOutputTokens);

        return with503Retries(() -> {
            log.info("Enviando petición al proveedor de IA (Modelo: {}).", model);
            return restClient.post()
                    .uri("/v1beta/openai/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        });
    }

    private <T> T with503Retries(Supplier<T> operation) {
        for (int attempt = 1; ; attempt++) {
            try {
                return operation.get();
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() != 503 || attempt >= retryMaxAttempts) {
                    throw e;
                }

                long backoffMs = Math.min(
                        Math.min(retryInitialBackoffMs, 5000) * (1L << Math.min(attempt - 1, 20)), 5000);
                log.warn("El proveedor IA respondió 503; reintento {} de {} en {} ms.",
                        attempt, retryMaxAttempts - 1, backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new AiGenerationException("Se interrumpió la espera para reintentar la llamada a la IA.",
                            interruptedException);
                }
            }
        }
    }

    private static RestClient createRestClient(String baseUrl, String apiKey, int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }
}