package com.quizly.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.quizly.app.dto.ai.PdfQuizGeneratorRequest;
import com.quizly.app.dto.form.OpcionRespuestaForm;
import com.quizly.app.dto.form.PreguntaForm;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.exception.AiGenerationException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class AiQuizResponseParser {

    private final ObjectMapper objectMapper;

    public AiQuizResponseParser() {
        this(JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build());
    }

    AiQuizResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String parseSummary(String rawJsonResponse) {
        try {
            JsonNode summary = objectMapper.readTree(extractCompletionContent(rawJsonResponse));
            if (summary.isMissingNode() || summary.isEmpty()) {
                throw new AiGenerationException("La IA devolvió un resumen vacío para un fragmento del PDF.");
            }
            return summary.toString();
        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new AiGenerationException("La IA devolvió un resumen no válido para un fragmento del PDF.", e);
        }
    }

    QuizForm parseQuiz(String rawJsonResponse, String originalFileName, PdfQuizGeneratorRequest request) {
        try {
            JsonNode quizNode = objectMapper.readTree(extractCompletionContent(rawJsonResponse));
            QuizForm form = new QuizForm();

            String titulo = quizNode.path("titulo").asText(null);
            if (StringUtils.isBlank(titulo)) {
                titulo = (originalFileName != null && !originalFileName.isBlank())
                        ? "Cuestionario de " + originalFileName.replaceFirst("(?i)\\.pdf$", "")
                        : "Cuestionario generado por IA";
            }
            if (titulo.length() > 100) {
                titulo = titulo.substring(0, 100);
            }
            form.setTitulo(titulo);

            String descripcion = quizNode.path("descripcion").asText(null);
            if (descripcion != null && descripcion.length() > 255) {
                descripcion = descripcion.substring(0, 255);
            }
            form.setDescripcion(descripcion);
            form.setDificultadId(request.getDificultadId());
            form.setSinLimiteTiempo(true);
            form.setPreguntasAleatorias(false);
            form.setEditable(true);

            List<PreguntaForm> preguntas = new ArrayList<>();
            JsonNode preguntasNode = quizNode.path("preguntas");
            if (preguntasNode.isArray()) {
                for (JsonNode preguntaNode : preguntasNode) {
                    preguntas.add(parseQuestion(preguntaNode, request));
                }
            }

            validateQuestionSet(preguntas, request);
            form.setPreguntas(preguntas);
            return form;
        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al parsear la respuesta JSON de la IA: {}", rawJsonResponse, e);
            throw new AiGenerationException("La IA devolvió un formato no válido: " + e.getMessage(), e);
        }
    }

    String getRequestedQuestionType(PdfQuizGeneratorRequest request) {
        String type = StringUtils.defaultIfBlank(request.getTipoPreguntas(), "VARIADAS").toUpperCase(Locale.ROOT);
        if (!List.of("VARIADAS", "SINGLE_CHOICE", "TRUE_FALSE").contains(type)) {
            throw new AiGenerationException("El tipo de preguntas solicitado no es válido.");
        }
        return type;
    }

    private String extractCompletionContent(String rawJsonResponse) throws Exception {
        JsonNode rootNode = objectMapper.readTree(rawJsonResponse);
        JsonNode choices = rootNode.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new AiGenerationException("La respuesta de la IA no contiene opciones válidas.");
        }

        String content = choices.get(0).path("message").path("content").asText();
        if (StringUtils.isBlank(content)) {
            throw new AiGenerationException("La IA devolvió un contenido de respuesta vacío.");
        }

        content = content.trim();
        if (content.startsWith("```")) {
            content = content.replaceAll("^```(?:json)?\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();
        }
        return content;
    }

    private PreguntaForm parseQuestion(JsonNode preguntaNode, PdfQuizGeneratorRequest request) {
        PreguntaForm pregunta = new PreguntaForm();
        String enunciado = preguntaNode.path("enunciado").asText("").trim();
        if (enunciado.length() > 255) {
            enunciado = enunciado.substring(0, 255);
        }
        pregunta.setEnunciado(enunciado);

        String tipo = preguntaNode.has("tipoPreguntaCodigo")
                ? preguntaNode.path("tipoPreguntaCodigo").asText()
                : preguntaNode.path("tipoPregunta").asText("SINGLE_CHOICE");
        if (!"SINGLE_CHOICE".equals(tipo) && !"TRUE_FALSE".equals(tipo)) {
            throw new AiGenerationException("La IA devolvió un tipo de pregunta no válido.");
        }

        String requestedType = getRequestedQuestionType(request);
        if (("TRUE_FALSE".equals(requestedType) && !"TRUE_FALSE".equals(tipo))
                || ("SINGLE_CHOICE".equals(requestedType) && !"SINGLE_CHOICE".equals(tipo))) {
            throw new AiGenerationException("La IA no respetó el tipo de preguntas solicitado.");
        }
        pregunta.setTipoPreguntaCodigo(tipo);

        JsonNode opcionesNode = preguntaNode.has("opcionesRespuesta")
                ? preguntaNode.path("opcionesRespuesta")
                : preguntaNode.path("opciones");
        List<OpcionRespuestaForm> opciones = new ArrayList<>();
        int correctCount = 0;
        if (opcionesNode.isArray()) {
            for (JsonNode optNode : opcionesNode) {
                OpcionRespuestaForm opcion = parseOption(optNode);
                if (opcion.isCorrecta()) {
                    correctCount++;
                }
                opciones.add(opcion);
            }
        }

        boolean validOptionCount = "TRUE_FALSE".equals(tipo)
                ? opciones.size() == 2
                : opciones.size() >= 3 && opciones.size() <= 4;
        if (!validOptionCount || correctCount != 1
                || opciones.stream().anyMatch(option -> StringUtils.isBlank(option.getTexto()))) {
            throw new AiGenerationException("La IA devolvió opciones de respuesta no válidas.");
        }

        pregunta.setOpcionesRespuesta(opciones);
        return pregunta;
    }

    private OpcionRespuestaForm parseOption(JsonNode optNode) {
        OpcionRespuestaForm opcion = new OpcionRespuestaForm();
        String texto = optNode.path("texto").asText("").trim();
        if (texto.length() > 255) {
            texto = texto.substring(0, 255);
        }
        opcion.setTexto(texto);
        opcion.setCorrecta(optNode.has("correcta")
                ? optNode.path("correcta").asBoolean(false)
                : optNode.path("esCorrecta").asBoolean(false));

        String retroalimentacion = optNode.path("retroalimentacion").asText(null);
        if (retroalimentacion != null && retroalimentacion.length() > 500) {
            retroalimentacion = retroalimentacion.substring(0, 500);
        }
        opcion.setRetroalimentacion(retroalimentacion);
        return opcion;
    }

    private void validateQuestionSet(List<PreguntaForm> preguntas, PdfQuizGeneratorRequest request) {
        if (preguntas.size() != request.getNumPreguntas()
                || preguntas.stream().anyMatch(question -> StringUtils.isBlank(question.getEnunciado()))) {
            throw new AiGenerationException("La IA no generó el número de preguntas válidas solicitado.");
        }

        if ("VARIADAS".equals(getRequestedQuestionType(request))) {
            long singleChoiceCount = preguntas.stream()
                    .filter(question -> "SINGLE_CHOICE".equals(question.getTipoPreguntaCodigo()))
                    .count();
            long trueFalseCount = preguntas.size() - singleChoiceCount;
            if (Math.abs(singleChoiceCount - trueFalseCount) > 1) {
                throw new AiGenerationException("La IA no generó una combinación equilibrada de tipos de pregunta.");
            }
        }
    }
}