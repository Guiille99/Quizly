package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiTokenCounterTest {

    @Test
    void cuentaTokensConElFormatoGeminiIncluyendoLaInstruccionDelSistema() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-test:countTokens"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("generateContentRequest"),
                    org.hamcrest.Matchers.containsString("models/gemini-test"),
                        org.hamcrest.Matchers.containsString("system instruction"),
                        org.hamcrest.Matchers.containsString("user prompt"))))
                .andRespond(withSuccess("{\"totalTokens\":42}", MediaType.APPLICATION_JSON));

        GeminiTokenCounter counter = new GeminiTokenCounter(builder.build());

        assertEquals(42, counter.countTokens("models/gemini-test", "system instruction", "user prompt"));
        server.verify();
    }
}