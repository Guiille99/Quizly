package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class GeminiApiClientTest {

    private static final String CHAT_URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

    @Test
    void conmutaAlSiguienteModeloCuandoElPrimeroDevuelve429() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // 1ª llamada: el modelo primario 'gemini-3.8-flash' falla con 429
        server.expect(requestTo(CHAT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("gemini-3.8-flash")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"Quota exceeded\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        // 2ª llamada en la misma operación: conmuta a 'gemini-3.7-flash' y tiene éxito
        server.expect(requestTo(CHAT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("gemini-3.7-flash")))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}", MediaType.APPLICATION_JSON));

        GeminiApiClient client = new GeminiApiClient(
                builder.build(),
                List.of("gemini-3.8-flash", "gemini-3.7-flash"),
                (model, system, prompt) -> 10,
                3,
                0,
                60);

        String result = client.generate("system prompt", "user prompt", 100);

        assertTrue(result.contains("OK"));
        server.verify();
    }

    @Test
    void saltaElModeloEnCooldownEnLlamadasPosteriores() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Operación 1: gemini-3.8-flash da 429, conmuta a gemini-3.7-flash y triunfa
        server.expect(requestTo(CHAT_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("gemini-3.8-flash")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\":{\"code\":429,\"message\":\"Quota exceeded\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        server.expect(requestTo(CHAT_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("gemini-3.7-flash")))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"Respuesta 1\"}}]}", MediaType.APPLICATION_JSON));

        // Operación 2: gemini-3.8-flash sigue en cooldown (60s), por lo que va directamente a gemini-3.7-flash
        server.expect(requestTo(CHAT_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("gemini-3.7-flash")))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"Respuesta 2\"}}]}", MediaType.APPLICATION_JSON));

        GeminiApiClient client = new GeminiApiClient(
                builder.build(),
                List.of("gemini-3.8-flash", "gemini-3.7-flash"),
                (model, system, prompt) -> 10,
                3,
                0,
                60);

        String res1 = client.generate("system prompt", "user prompt 1", 100);
        String res2 = client.generate("system prompt", "user prompt 2", 100);

        assertTrue(res1.contains("Respuesta 1"));
        assertTrue(res2.contains("Respuesta 2"));
        server.verify();
    }

    @Test
    void lanzaExcepcion429CuandoTodosLosModelosAgotanSuCuota() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo(CHAT_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("gemini-3.8-flash")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        server.expect(requestTo(CHAT_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("gemini-3.7-flash")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        GeminiApiClient client = new GeminiApiClient(
                builder.build(),
                List.of("gemini-3.8-flash", "gemini-3.7-flash"),
                (model, system, prompt) -> 10,
                3,
                0,
                60);

        RestClientResponseException ex = assertThrows(RestClientResponseException.class, () ->
                client.generate("system", "prompt", 100));

        assertEquals(429, ex.getStatusCode().value());
        server.verify();
    }

    @Test
    void realizaFallbackEnConteoDeTokensSiElModeloInicialDevuelve429() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");

        AiTokenCounter mockTokenCounter = (model, system, prompt) -> {
            if ("gemini-3.8-flash".equals(model)) {
                throw new org.springframework.web.client.HttpClientErrorException(
                        HttpStatus.TOO_MANY_REQUESTS, "Quota exceeded");
            }
            if ("gemini-3.7-flash".equals(model)) {
                return 42;
            }
            throw new IllegalArgumentException("Modelo inesperado: " + model);
        };

        GeminiApiClient client = new GeminiApiClient(
                builder.build(),
                List.of("gemini-3.8-flash", "gemini-3.7-flash"),
                mockTokenCounter,
                3,
                0,
                60);

        int tokenCount = client.countTokens("system instruction", "user prompt");

        assertEquals(42, tokenCount);
    }

    @Test
    void normalizaModelosYAliasCorrectamente() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");

        GeminiApiClient client = new GeminiApiClient(
                builder.build(),
                List.of("models/gemini-3.8-flash", " gemini-3-flash ", "gemini-3.5-flash-lite"),
                (model, system, prompt) -> 10,
                3,
                0,
                60);

        List<String> models = client.getModels();

        assertEquals(3, models.size());
        assertEquals("gemini-3.8-flash", models.get(0));
        assertEquals("gemini-3-flash-preview", models.get(1));
        assertEquals("gemini-3.5-flash-lite", models.get(2));
    }

    @Test
    void conmutaAlSiguienteModeloCuandoElPrimeroSufreTimeout() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // 1ª llamada: el modelo primario 'gemini-3.8-flash' sufre un SocketTimeoutException
        server.expect(requestTo(CHAT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("gemini-3.8-flash")))
                .andRespond(request -> {
                    throw new org.springframework.web.client.ResourceAccessException(
                            "I/O error on POST request: Read timed out",
                            new java.net.SocketTimeoutException("Read timed out"));
                });

        // 2ª llamada: conmuta a 'gemini-3.7-flash' y responde con éxito
        server.expect(requestTo(CHAT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("gemini-3.7-flash")))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"OK tras timeout\"}}]}", MediaType.APPLICATION_JSON));

        GeminiApiClient client = new GeminiApiClient(
                builder.build(),
                List.of("gemini-3.8-flash", "gemini-3.7-flash"),
                (model, system, prompt) -> 10,
                3,
                0,
                60);

        String result = client.generate("system prompt", "user prompt", 100);

        assertTrue(result.contains("OK tras timeout"));
        server.verify();
    }
}

