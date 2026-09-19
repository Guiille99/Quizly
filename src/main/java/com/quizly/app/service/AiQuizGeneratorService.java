package com.quizly.app.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.quizly.app.dto.ai.PdfQuizGeneratorRequest;
import com.quizly.app.dto.form.OpcionRespuestaForm;
import com.quizly.app.dto.form.PreguntaForm;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.exception.AiGenerationException;
import com.quizly.app.exception.AiHighDemandException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Slf4j 
@Service 
public class AiQuizGeneratorService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    @Autowired 
    public AiQuizGeneratorService(
            @Value("${ai.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:gemini-1.5-flash}") String model,
            @Value("${ai.timeout-seconds:45}") int timeoutSeconds) {
        
        this.model = model;
        this.objectMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }

    /**
     * Constructor para pruebas unitarias con RestClient simulado o preconfigurado.
     */
    AiQuizGeneratorService(RestClient restClient, ObjectMapper objectMapper, String model) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    /**
     * Genera la estructura completa de un QuizForm pre-poblado basándose en el texto extraído del PDF.
     *
     * @param extractedText Texto extraído del documento
     * @param request Parámetros de generación solicitados por el usuario
     * @param filename Nombre original del archivo PDF subido
     * @return QuizForm con las preguntas y opciones generadas
     */
    public QuizForm generateQuizFromText(String extractedText, PdfQuizGeneratorRequest request, String filename) {
        String prompt = buildSystemAndUserPrompt(extractedText, request);

        Map<String, Object> requestBody = Map.of(
            "model", this.model,
            "messages", List.of(
                Map.of("role", "system", "content", getSystemInstruction()),
                Map.of("role", "user", "content", prompt)
            ),
            "response_format", Map.of("type", "json_object"),
            "temperature", 0.3
        );

        try {
            log.info("Enviando petición de generación a la API de IA (Modelo: {})...", this.model);

            String jsonResponse = restClient.post()
                .uri("/v1beta/openai/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(String.class);

            return parseAiResponseToQuizForm(jsonResponse, filename, request);
        } catch(RestClientResponseException e){
            log.error("Error HTTP del proveedor IA [{}] Body: {}", e.getStatusCode(), e.getResponseBodyAsString());

            int statusCode = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString() != null ? e.getResponseBodyAsString() : "";

            // Detección de alta demanda o cuotas agotadas
            if (statusCode == 429 || statusCode == 503 || responseBody.contains("RESOURCE_EXHAUSTED") || responseBody.contains("UNAVAILABLE")) {
                throw new AiHighDemandException("El modelo de IA está experimentando una alta demanda en este momento");
            }
            
            throw new AiGenerationException(e.getMessage());
        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al comunicarse con el proveedor de IA", e);
            throw new AiGenerationException(e.getMessage());        
        }
    }

    private String getSystemInstruction() {
        return """
                Eres un experto pedagogo y diseñador de exámenes académicos.
                Tu tarea es generar un cuestionario riguroso y de alta calidad pedagógica en formato JSON estricto a partir del texto proporcionado.

                Estructura JSON obligatoria requerida:
                {
                    "titulo": "Título representativo y conciso",
                    "descripcion": "Breve descripción general del tema evaluado",
                    "preguntas": [
                        {
                            "enunciado": "Texto claro y preciso de la pregunta",
                            "tipoPregunta": "SINGLE_CHOICE",
                            "opciones": [
                                {
                                    "texto": "Texto de la opción",
                                    "correcta": true,
                                    "retroalimentacion": "Explicación de por qué esta opción es correcta o incorrecta"
                                },
                                {
                                    "texto": "Texto de la opción alternativa",
                                    "correcta": false,
                                    "retroalimentacion": "Explicación de por qué esta opción es incorrecta"
                                }
                            ]
                        }
                    ]
                }

                REGLAS ESTRICTAS:
                1. Genera exactamente el número de preguntas solicitadas.
                2. Para 'SINGLE_CHOICE', incluye entre 3 y 4 opciones de respuesta. EXACTAMENTE 1 opción debe tener 'correcta': true.
                3. Para 'TRUE_FALSE', incluye exactamente 2 opciones ('Verdadero' y 'Falso'). EXACTAMENTE 1 debe tener 'correcta': true.
                4. Todas las preguntas y opciones deben estar redactadas en español y basarse únicamente en la información del texto proporcionado.
                5. Proporciona una retroalimentación concisa y formativa para cada opción.
                6. Responde ÚNICAMENTE con el objeto JSON válido. No incluyas bloques markdown ni explicaciones adicionales fuera del JSON.
                """;
    }

    private String buildSystemAndUserPrompt(String text, PdfQuizGeneratorRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Por favor, genera un cuestionario con las siguientes especificaciones:\n");
        sb.append("- Número de preguntas: ").append(request.getNumPreguntas()).append("\n");

        if (StringUtils.isNotBlank(request.getIndicacionesAdicionales())) {
            sb.append("- Enfoque/Indicaciones especiales: ").append(request.getIndicacionesAdicionales().trim()).append("\n");
        }

        sb.append("\nTEXTO FUENTE EXTRAÍDO DEL DOCUMENTO:\n");

        int maxChars = Math.min(text.length(), 15000);
        sb.append(text, 0, maxChars);

        return sb.toString();
    }

    private QuizForm parseAiResponseToQuizForm(String rawJsonResponse, String originalFileName, PdfQuizGeneratorRequest request) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawJsonResponse);
            JsonNode choices = rootNode.path("choices");

            if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                throw new AiGenerationException("La respuesta de la IA no contiene opciones válidas.");
            }

            String contentJson = choices.get(0).path("message").path("content").asText();
            if (StringUtils.isBlank(contentJson)) {
                throw new AiGenerationException("La IA devolvió un contenido de respuesta vacío.");
            }

            contentJson = contentJson.trim();
            if (contentJson.startsWith("```")) {
                contentJson = contentJson.replaceAll("^```(?:json)?\\s*", "")
                                         .replaceAll("\\s*```$", "")
                                         .trim();
            }

            JsonNode quizNode = objectMapper.readTree(contentJson);
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

            List<PreguntaForm> preguntasList = new ArrayList<>();
            JsonNode preguntasNode = quizNode.path("preguntas");

            if (preguntasNode.isArray()) {
                for (JsonNode preguntaNode : preguntasNode) {
                    PreguntaForm pf = new PreguntaForm();
                    String enunciado = preguntaNode.path("enunciado").asText("").trim();
                    if (enunciado.length() > 255) {
                        enunciado = enunciado.substring(0, 255);
                    }
                    pf.setEnunciado(enunciado);

                    String tipo = preguntaNode.has("tipoPreguntaCodigo")
                        ? preguntaNode.path("tipoPreguntaCodigo").asText()
                        : preguntaNode.path("tipoPregunta").asText("SINGLE_CHOICE");

                    if (!"SINGLE_CHOICE".equals(tipo) && !"TRUE_FALSE".equals(tipo)) {
                        tipo = "SINGLE_CHOICE";
                    }
                    pf.setTipoPreguntaCodigo(tipo);

                    JsonNode opcionesNode = preguntaNode.has("opcionesRespuesta")
                        ? preguntaNode.path("opcionesRespuesta")
                        : preguntaNode.path("opciones");

                    List<OpcionRespuestaForm> opcionesList = new ArrayList<>();
                    int correctCount = 0;

                    if (opcionesNode.isArray()) {
                        for (JsonNode optNode : opcionesNode) {
                            OpcionRespuestaForm opt = new OpcionRespuestaForm();
                            String texto = optNode.path("texto").asText("").trim();
                            if (texto.length() > 255) {
                                texto = texto.substring(0, 255);
                            }
                            opt.setTexto(texto);

                            boolean correcta = optNode.has("correcta")
                                ? optNode.path("correcta").asBoolean(false)
                                : optNode.path("esCorrecta").asBoolean(false);

                            if (correcta) {
                                correctCount++;
                                if (correctCount > 1) {
                                    correcta = false;
                                }
                            }
                            opt.setCorrecta(correcta);

                            String retro = optNode.path("retroalimentacion").asText(null);
                            if (retro != null && retro.length() > 500) {
                                retro = retro.substring(0, 500);
                            }
                            opt.setRetroalimentacion(retro);

                            opcionesList.add(opt);
                        }
                    }

                    if (correctCount == 0 && !opcionesList.isEmpty()) {
                        opcionesList.get(0).setCorrecta(true);
                    }

                    pf.setOpcionesRespuesta(opcionesList);

                    if (StringUtils.isNotBlank(pf.getEnunciado()) && pf.getOpcionesRespuesta().size() >= 2) {
                        preguntasList.add(pf);
                    }
                }
            }

            if (preguntasList.isEmpty()) {
                throw new AiGenerationException("La IA no generó ninguna pregunta válida estructurada.");
            }

            form.setPreguntas(preguntasList);
            return form;

        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al parsear la respuesta JSON de la IA: {}", rawJsonResponse, e);
            throw new AiGenerationException("La IA devolvió un formato no válido: " + e.getMessage(), e);
        }
    }
}
