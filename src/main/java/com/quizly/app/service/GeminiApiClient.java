package com.quizly.app.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.quizly.app.exception.AiGenerationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GeminiApiClient {

    private final RestClient restClient;
    private final List<String> models;
    private final AiTokenCounter tokenCounter;
    private final int retryMaxAttempts;
    private final long retryInitialBackoffMs;
    private final int cooldownSeconds;
    private final Map<String, Instant> modelCooldowns = new ConcurrentHashMap<>();

    @Autowired
    public GeminiApiClient(
            @Value("${ai.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.models:gemini-3.8-flash,gemini-3.7-flash,gemini-3.5-flash-lite,gemini-3.6-flash,gemini-3.1-flash-lite,gemini-3-flash-preview}") List<String> models,
            @Value("${ai.cooldown-seconds:60}") int cooldownSeconds,
            @Value("${ai.timeout-seconds:90}") int timeoutSeconds,
            @Value("${ai.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${ai.retry.initial-backoff-ms:500}") long retryInitialBackoffMs,
            AiTokenCounter tokenCounter) {
        this(models, tokenCounter, retryMaxAttempts, retryInitialBackoffMs, cooldownSeconds,
                createRestClient(baseUrl, apiKey, timeoutSeconds));
    }

    GeminiApiClient(
            List<String> models,
            AiTokenCounter tokenCounter,
            int retryMaxAttempts,
            long retryInitialBackoffMs,
            int cooldownSeconds,
            RestClient restClient) {
        this.restClient = restClient;
        this.models = parseModels(models);
        this.tokenCounter = tokenCounter;
        this.retryMaxAttempts = Math.max(1, retryMaxAttempts);
        this.retryInitialBackoffMs = Math.max(0, retryInitialBackoffMs);
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
    }

    GeminiApiClient(
            RestClient restClient,
            String model,
            AiTokenCounter tokenCounter,
            int retryMaxAttempts,
            long retryInitialBackoffMs) {
        this(List.of(model), tokenCounter, retryMaxAttempts, retryInitialBackoffMs, 60, restClient);
    }

    GeminiApiClient(
            RestClient restClient,
            List<String> models,
            AiTokenCounter tokenCounter,
            int retryMaxAttempts,
            long retryInitialBackoffMs,
            int cooldownSeconds) {
        this(models, tokenCounter, retryMaxAttempts, retryInitialBackoffMs, cooldownSeconds, restClient);
    }

    public List<String> getModels() {
        return models;
    }

    public int countTokens(String systemInstruction, String userPrompt) {
        return executeWithFallback("conteo de tokens", currentModel ->
                with503Retries(currentModel, () -> tokenCounter.countTokens(currentModel, systemInstruction, userPrompt))
        );
    }

    public String generate(String systemInstruction, String userPrompt, int maxOutputTokens) {
        return executeWithFallback("generación de contenido", currentModel -> {
            Map<String, Object> requestBody = Map.of(
                    "model", currentModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", userPrompt)),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.3,
                    "max_tokens", maxOutputTokens);

            return with503Retries(currentModel, () -> {
                log.info("Enviando petición al proveedor de IA (Modelo: {}).", currentModel);
                String response = restClient.post()
                        .uri("/v1beta/openai/chat/completions")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                log.info("Petición procesada con éxito (Modelo: {})", currentModel);
                return response;
            });
        });
    }

    private <T> T executeWithFallback(String operationName, ModelOperation<T> operation) {
        List<String> candidates = getOrderedCandidates();
        RestClientException lastFailoverException = null;

        for (int i = 0; i < candidates.size(); i++) {
            String currentModel = candidates.get(i);
            try {
                return operation.execute(currentModel);
            } catch (RestClientException e) {
                if (isFailoverEligible(e)) {
                    lastFailoverException = e;
                    Instant cooldownUntil = Instant.now().plusSeconds(cooldownSeconds);
                    modelCooldowns.put(currentModel, cooldownUntil);

                    String reason = getFailoverReason(e);
                    if (i + 1 < candidates.size()) {
                        String nextModel = candidates.get(i + 1);
                        log.warn("El modelo '{}' no pudo completar la {} ({}) y se pone en enfriamiento durante {}s. "
                                + "Conmutando a modelo alternativo: '{}'.",
                                currentModel, operationName, reason, cooldownSeconds, nextModel);
                    } else {
                        log.error("Todos los modelos de IA configurados han fallado para {}. Último motivo: {}. Modelos intentados: {}",
                                operationName, reason, candidates);
                    }
                } else {
                    throw e;
                }
            }
        }

        if (lastFailoverException != null) {
            throw lastFailoverException;
        }
        throw new IllegalStateException("No hay modelos de IA disponibles para ejecutar la operación: " + operationName);
    }

    private List<String> getOrderedCandidates() {
        Instant now = Instant.now();
        List<String> available = new ArrayList<>();
        List<String> coolingDown = new ArrayList<>();

        for (String model : models) {
            Instant cooldownUntil = modelCooldowns.get(model);
            if (cooldownUntil == null || now.isAfter(cooldownUntil)) {
                available.add(model);
            } else {
                coolingDown.add(model);
            }
        }

        coolingDown.sort(Comparator.comparing(m -> modelCooldowns.getOrDefault(m, Instant.MIN)));

        List<String> candidates = new ArrayList<>(available);
        candidates.addAll(coolingDown);
        return candidates;
    }

    private boolean isFailoverEligible(RestClientException e) {
        if (e instanceof RestClientResponseException respEx) {
            int status = respEx.getStatusCode().value();
            if (status == 429 || status == 503) {
                return true;
            }
            String body = respEx.getResponseBodyAsString();
            if (body != null && (body.contains("RESOURCE_EXHAUSTED")
                    || body.contains("RATE_LIMIT_EXCEEDED")
                    || body.contains("UNAVAILABLE"))) {
                return true;
            }
        }
        return isTimeout(e);
    }

    private boolean isTimeout(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            String msg = current.getMessage();
            if (msg != null && (msg.toLowerCase().contains("timed out") || msg.toLowerCase().contains("timeout"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String getFailoverReason(RestClientException e) {
        if (isTimeout(e)) {
            return "Timeout de lectura/conexión";
        }
        if (e instanceof RestClientResponseException respEx) {
            return "HTTP " + respEx.getStatusCode().value() + " (" + respEx.getStatusText() + ")";
        }
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private <T> T with503Retries(String modelName, Supplier<T> operation) {
        for (int attempt = 1; ; attempt++) {
            try {
                return operation.get();
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() != 503 || attempt >= retryMaxAttempts) {
                    throw e;
                }

                long backoffMs = Math.min(
                        Math.min(retryInitialBackoffMs, 5000) * (1L << Math.min(attempt - 1, 20)), 5000);
                log.warn("El proveedor IA respondió 503 para el modelo {}; reintento {} de {} en {} ms.",
                        modelName, attempt, retryMaxAttempts - 1, backoffMs);
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

    private static List<String> parseModels(List<String> rawModels) {
        if (rawModels == null || rawModels.isEmpty()) {
            return List.of(
                    "gemini-3.8-flash",
                    "gemini-3.7-flash",
                    "gemini-3.5-flash-lite",
                    "gemini-3.6-flash",
                    "gemini-3.1-flash-lite",
                    "gemini-3-flash-preview");
        }
        List<String> list = new ArrayList<>();
        for (String entry : rawModels) {
            if (entry != null) {
                for (String part : entry.split(",")) {
                    String normalized = normalizeModel(part);
                    if (!normalized.isBlank() && !list.contains(normalized)) {
                        list.add(normalized);
                    }
                }
            }
        }
        return list.isEmpty() ? List.of("gemini-3.8-flash") : Collections.unmodifiableList(list);
    }

    private static String normalizeModel(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("models/")) {
            trimmed = trimmed.substring("models/".length());
        }
        if ("gemini-3-flash".equalsIgnoreCase(trimmed) || "gemini-3.0-flash".equalsIgnoreCase(trimmed)) {
            return "gemini-3-flash-preview";
        }
        return trimmed;
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

    @FunctionalInterface
    private interface ModelOperation<T> {
        T execute(String model);
    }
}