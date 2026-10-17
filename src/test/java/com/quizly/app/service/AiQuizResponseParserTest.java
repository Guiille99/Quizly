package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.quizly.app.dto.ai.PdfQuizGeneratorRequest;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.exception.AiGenerationException;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class AiQuizResponseParserTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final AiQuizResponseParser parser = new AiQuizResponseParser(objectMapper);

    @Test
    void parseaUnCuestionarioValidoYAplicaLaDificultadSolicitada() throws Exception {
        String response = completion(quiz("SINGLE_CHOICE", 3));
        PdfQuizGeneratorRequest request = request("SINGLE_CHOICE", 3);
        request.setDificultadId(4L);

        QuizForm form = parser.parseQuiz(response, "unidad.pdf", request);

        assertEquals("Cuestionario de prueba", form.getTitulo());
        assertEquals(4L, form.getDificultadId());
        assertEquals(3, form.getPreguntas().size());
        assertEquals(3, form.getPreguntas().get(0).getOpcionesRespuesta().size());
    }

    @Test
    void rechazaUnaCantidadDistintaALaSolicitada() throws Exception {
        assertThrows(AiGenerationException.class,
                () -> parser.parseQuiz(completion(quiz("SINGLE_CHOICE", 2)), "unidad.pdf",
                        request("SINGLE_CHOICE", 3)));
    }

    @Test
    void rechazaTiposNoEquilibradosCuandoSeSolicitanPreguntasVariadas() throws Exception {
        assertThrows(AiGenerationException.class,
                () -> parser.parseQuiz(completion(quiz("SINGLE_CHOICE", 3)), "unidad.pdf",
                        request("VARIADAS", 3)));
    }

    @Test
    void rechazaUnaPreguntaSinUnaUnicaOpcionCorrecta() throws Exception {
        Map<String, Object> quiz = quiz("SINGLE_CHOICE", 3);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) quiz.get("preguntas");
        questions.get(0).put("opciones", List.of(
                Map.of("texto", "Primera", "correcta", true),
                Map.of("texto", "Segunda", "correcta", true),
                Map.of("texto", "Tercera", "correcta", false)));

        assertThrows(AiGenerationException.class,
                () -> parser.parseQuiz(completion(quiz), "unidad.pdf", request("SINGLE_CHOICE", 3)));
    }

    @Test
    void rechazaRespuestasTruncadasPorLimiteDeTokens() throws Exception {
        String truncatedCompletion = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "finish_reason", "length",
                        "message", Map.of("content", "{\"titulo\": \"Incompleto\"")))));

        AiGenerationException ex = assertThrows(AiGenerationException.class,
                () -> parser.parseQuiz(truncatedCompletion, "unidad.pdf", request("SINGLE_CHOICE", 3)));

        assertEquals(
                "La respuesta de la IA excedió el límite máximo de tokens y quedó incompleta. Pruebe a solicitar menos preguntas o aumente el límite de tokens.",
                ex.getMessage());
    }

    private PdfQuizGeneratorRequest request(String type, int questionCount) {
        return PdfQuizGeneratorRequest.builder()
                .pdfFile(new MockMultipartFile("file", "unidad.pdf", "application/pdf", new byte[] { 1 }))
                .numPreguntas(questionCount)
                .tipoPreguntas(type)
                .build();
    }

    private Map<String, Object> quiz(String type, int questionCount) {
        List<Map<String, Object>> questions = new java.util.ArrayList<>();
        for (int index = 1; index <= questionCount; index++) {
            questions.add(new java.util.LinkedHashMap<>(Map.of(
                    "enunciado", "Pregunta " + index,
                    "tipoPregunta", type,
                    "opciones", List.of(
                            Map.of("texto", "Correcta", "correcta", true),
                            Map.of("texto", "Alternativa A", "correcta", false),
                            Map.of("texto", "Alternativa B", "correcta", false)))));
        }
        return new java.util.LinkedHashMap<>(Map.of(
                "titulo", "Cuestionario de prueba",
                "descripcion", "Descripción",
                "preguntas", questions));
    }

    private String completion(Map<String, Object> quiz) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", objectMapper.writeValueAsString(quiz))))));
    }
}