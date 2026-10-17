package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.mock.web.MockMultipartFile;

import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.exception.AiGenerationException;
import com.quizly.app.exception.AiHighDemandException;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class AiQuizGeneratorServiceTest {

    private static final String CHAT_URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    private static final String SUMMARY_RESPONSE = "{\"choices\":[{\"message\":{\"content\":\"{\\\"resumen\\\":\\\"Cobertura\\\",\\\"conceptosClave\\\":[\\\"tema\\\"],\\\"hechosPreguntables\\\":[\\\"hecho\\\"]}\"}}]}";

    @Test
    void enviaTodoElTextoEnUnaLlamadaCuandoCabeAunqueSupereQuinceMilCaracteres() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String fullText = "INICIO_DOCUMENTO " + "contenido del documento ".repeat(1000) + "FIN_DOCUMENTO";
        server.expect(requestTo(CHAT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FIN_DOCUMENTO")))
                .andRespond(withSuccess(quizCompletion(objectMapper, 3, "SINGLE_CHOICE"), MediaType.APPLICATION_JSON));

        AiQuizGeneratorService service = createService(
            builder, objectMapper, (model, system, prompt) -> 100, 100000);
        QuizForm result = service.generateQuizFromText(fullText, request("SINGLE_CHOICE", 3), "apuntes.pdf");

        assertEquals(3, result.getPreguntas().size());
        server.verify();
    }

    @Test
    void procesaPaginasInicialesYFinalesYConsolidaLosFragmentos() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        for (int pageNumber = 1; pageNumber <= 5; pageNumber++) {
            server.expect(requestTo(CHAT_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("PAGINA_" + pageNumber)))
                    .andRespond(withSuccess(SUMMARY_RESPONSE, MediaType.APPLICATION_JSON));
        }
        server.expect(requestTo(CHAT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(quizCompletion(objectMapper, 3, "SINGLE_CHOICE"), MediaType.APPLICATION_JSON));

        AiQuizGeneratorService service = createService(builder, objectMapper,
            (model, system, prompt) -> prompt.length() > 3000 ? 1000 : 100);
        List<String> pages = new ArrayList<>();
        for (int pageNumber = 1; pageNumber <= 5; pageNumber++) {
            pages.add("PAGINA_" + pageNumber + " " + "contenido academico ".repeat(100));
        }

        QuizForm result = service.generateQuizFromText(String.join("\f", pages), request("SINGLE_CHOICE", 3), "apuntes.pdf");

        assertEquals(3, result.getPreguntas().size());
        server.verify();
    }

    @Test
    void rechazaUnaRespuestaConMenosPreguntasQueLasSolicitadas() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(quizCompletion(objectMapper, 2, "SINGLE_CHOICE"), MediaType.APPLICATION_JSON));

        AiQuizGeneratorService service = createService(builder, objectMapper, (model, system, prompt) -> 100);

        assertThrows(AiGenerationException.class,
                () -> service.generateQuizFromText("Texto fuente completo", request("SINGLE_CHOICE", 3), "apuntes.pdf"));
        server.verify();
    }

    @Test
    void rechazaUnaCombinacionNoEquilibradaCuandoSeSolicitanPreguntasVariadas() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(quizCompletion(objectMapper, 3, "SINGLE_CHOICE"), MediaType.APPLICATION_JSON));

        AiQuizGeneratorService service = createService(builder, objectMapper, (model, system, prompt) -> 100);

        assertThrows(AiGenerationException.class,
                () -> service.generateQuizFromText("Texto fuente completo", request("VARIADAS", 3), "apuntes.pdf"));
        server.verify();
    }

        @Test
        void reintentaUn503YContinuaCuandoElProveedorSeRecupera() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(CHAT_URL))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                .withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(CHAT_URL))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                .withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(CHAT_URL))
            .andRespond(withSuccess(quizCompletion(objectMapper, 3, "SINGLE_CHOICE"), MediaType.APPLICATION_JSON));

        AiQuizGeneratorService service = createService(builder, objectMapper,
            (model, system, prompt) -> 100, 1000, 3, 0);

        QuizForm result = service.generateQuizFromText(
            "Texto fuente completo", request("SINGLE_CHOICE", 3), "apuntes.pdf");

        assertEquals(3, result.getPreguntas().size());
        server.verify();
        }

        @Test
        void dejaDeReintentarTrasTresRespuestas503() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        for (int attempt = 0; attempt < 3; attempt++) {
            server.expect(requestTo(CHAT_URL))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                    .withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        }

        AiQuizGeneratorService service = createService(builder, objectMapper,
            (model, system, prompt) -> 100, 1000, 3, 0);

        assertThrows(AiHighDemandException.class,
            () -> service.generateQuizFromText("Texto fuente completo", request("SINGLE_CHOICE", 3), "apuntes.pdf"));
        server.verify();
        }

        @Test
        void subdivideUnaPaginaQuePorSiSolaExcedeElContextoSinPerderSusExtremos() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(CHAT_URL))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("INICIO_PAGINA")))
            .andRespond(withSuccess(SUMMARY_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(CHAT_URL))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("FINAL_PAGINA")))
            .andRespond(withSuccess(SUMMARY_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(CHAT_URL))
            .andRespond(withSuccess(quizCompletion(objectMapper, 3, "SINGLE_CHOICE"), MediaType.APPLICATION_JSON));

        AiQuizGeneratorService service = createService(builder, objectMapper,
            (model, system, prompt) -> (system.length() + prompt.length() + 3) / 4);
        String pageText = "INICIO_PAGINA " + "contenido academico ".repeat(220) + " FINAL_PAGINA";

        QuizForm result = service.generateQuizFromText(pageText, request("SINGLE_CHOICE", 3), "apuntes.pdf");

        assertEquals(3, result.getPreguntas().size());
        server.verify();
        }

        @Test
        void reduceJerarquicamenteLasNotasSiNoCabenEnElPromptFinal() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        for (int pageNumber = 1; pageNumber <= 3; pageNumber++) {
            server.expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(SUMMARY_RESPONSE, MediaType.APPLICATION_JSON));
        }
        server.expect(requestTo(CHAT_URL))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Integra estas notas")))
            .andRespond(withSuccess(SUMMARY_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(CHAT_URL))
            .andRespond(withSuccess(quizCompletion(objectMapper, 3, "SINGLE_CHOICE"), MediaType.APPLICATION_JSON));

        AiQuizGeneratorService service = createService(builder, objectMapper, (model, system, prompt) -> {
            if (prompt.startsWith("Por favor, genera")) {
            return 1000;
            }
            if (prompt.startsWith("Genera el cuestionario")
                    && prompt.indexOf("Cobertura") != prompt.lastIndexOf("Cobertura")) {
                return 1000;
            }
            if (prompt.startsWith("FRAGMENTO DEL DOCUMENTO")) {
                return 100;
            }
            if (prompt.contains("[PÁGINA 1]") || prompt.contains("[PÁGINA 2]") || prompt.contains("[PÁGINA 3]")) {
                return prompt.split("\\[PÁGINA ").length > 2 ? 1000 : 100;
            }
            return 100;
        });
        String documentText = String.join("\f", List.of("pagina uno", "pagina dos", "pagina tres"));

        QuizForm result = service.generateQuizFromText(documentText, request("SINGLE_CHOICE", 3), "apuntes.pdf");

        assertEquals(3, result.getPreguntas().size());
        server.verify();
        }

    private AiQuizGeneratorService createService(
            RestClient.Builder builder, ObjectMapper objectMapper, AiTokenCounter tokenCounter) {
        return createService(builder, objectMapper, tokenCounter, 1000, 3, 0);
        }

        private AiQuizGeneratorService createService(
            RestClient.Builder builder, ObjectMapper objectMapper, AiTokenCounter tokenCounter, int contextWindowTokens) {
        return createService(builder, objectMapper, tokenCounter, contextWindowTokens, 3, 0);
    }

    private AiQuizGeneratorService createService(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            AiTokenCounter tokenCounter,
            int contextWindowTokens,
            int retryMaxAttempts,
            long retryInitialBackoffMs) {
        GeminiApiClient geminiApiClient = new GeminiApiClient(
            builder.build(), "gemini-test", tokenCounter, retryMaxAttempts, retryInitialBackoffMs);
        AiContextPlanner contextPlanner = new AiContextPlanner(geminiApiClient, contextWindowTokens, 100, 0.1);
        return new AiQuizGeneratorService(geminiApiClient, contextPlanner, new AiQuizResponseParser(objectMapper));
    }

    private com.quizly.app.dto.ai.PdfQuizGeneratorRequest request(String type, int questionCount) {
        return com.quizly.app.dto.ai.PdfQuizGeneratorRequest.builder()
                .pdfFile(new MockMultipartFile("file", "apuntes.pdf", "application/pdf", new byte[] { 1 }))
                .numPreguntas(questionCount)
                .tipoPreguntas(type)
                .build();
    }

    private static String quizCompletion(ObjectMapper objectMapper, int questionCount, String type) throws Exception {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int questionNumber = 1; questionNumber <= questionCount; questionNumber++) {
            questions.add(Map.of(
                    "enunciado", "Pregunta " + questionNumber,
                    "tipoPregunta", type,
                    "opciones", List.of(
                            Map.of("texto", "Opción correcta", "correcta", true),
                            Map.of("texto", "Opción alternativa 1", "correcta", false),
                            Map.of("texto", "Opción alternativa 2", "correcta", false))));
        }
        Map<String, Object> quiz = new LinkedHashMap<>();
        quiz.put("titulo", "Cuestionario de prueba");
        quiz.put("descripcion", "Prueba de generación");
        quiz.put("preguntas", questions);
        String content = objectMapper.writeValueAsString(quiz);
        return objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", content)))));
    }
}