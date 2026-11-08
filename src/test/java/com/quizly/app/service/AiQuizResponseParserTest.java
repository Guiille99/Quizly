package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.quizly.app.dto.ai.MazoGenerado;
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

    // ---------------------------------------------------------------------
    // Tarjetas
    // ---------------------------------------------------------------------

    private String rawCompletion(String content) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("choices", List.of(Map.of("message", Map.of("content", content), "finish_reason", "stop"))));
    }

    @Test
    void parseaUnMazoValidoYNormalizaElTexto() throws Exception {
        String json = """
                {"titulo": "  Redes  ", "tarjetas": [
                    {"anverso": "¿Qué es   TCP?", "reverso": "Un protocolo fiable"},
                    {"anverso": "¿Qué es UDP?", "reverso": "Un protocolo sin conexión"}]}""";

        MazoGenerado mazo = parser.parseFlashcards(rawCompletion(json), "redes.pdf");

        assertEquals("Redes", mazo.titulo());
        assertEquals(2, mazo.tarjetas().size());
        assertEquals("¿Qué es TCP?", mazo.tarjetas().get(0).anverso());
    }

    @Test
    void aceptaElJsonEnvueltoEnUnBloqueMarkdown() throws Exception {
        String json = "```json\n{\"titulo\":\"T\",\"tarjetas\":[{\"anverso\":\"a\",\"reverso\":\"b\"}]}\n```";

        assertEquals(1, parser.parseFlashcards(rawCompletion(json), "x.pdf").tarjetas().size());
    }

    @Test
    void descartaLasTarjetasIncompletasYUsaElNombreDelFicheroSiNoHayTitulo() throws Exception {
        String json = """
                {"tarjetas": [{"anverso": "a", "reverso": ""}, {"anverso": " ", "reverso": "b"},
                              {"anverso": "ok", "reverso": "ok"}]}""";

        MazoGenerado mazo = parser.parseFlashcards(rawCompletion(json), "apuntes.PDF");

        assertEquals("Tarjetas de apuntes", mazo.titulo());
        assertEquals(1, mazo.tarjetas().size());
    }

    @Test
    void recortaAlMaximoDeTarjetas() throws Exception {
        StringBuilder tarjetas = new StringBuilder();
        for (int i = 0; i < AiQuizResponseParser.MAX_TARJETAS + 10; i++) {
            tarjetas.append(i > 0 ? "," : "").append("{\"anverso\":\"a").append(i).append("\",\"reverso\":\"b\"}");
        }

        MazoGenerado mazo = parser.parseFlashcards(
                rawCompletion("{\"titulo\":\"T\",\"tarjetas\":[" + tarjetas + "]}"), "x.pdf");

        assertEquals(AiQuizResponseParser.MAX_TARJETAS, mazo.tarjetas().size());
    }

    @Test
    void rechazaUnMazoSinTarjetasValidasOUnJsonInvalido() throws Exception {
        assertThrows(AiGenerationException.class,
                () -> parser.parseFlashcards(rawCompletion("{\"titulo\":\"T\",\"tarjetas\":[]}"), "x.pdf"));
        assertThrows(AiGenerationException.class,
                () -> parser.parseFlashcards(rawCompletion("esto no es json"), "x.pdf"));
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