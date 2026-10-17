package com.quizly.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.quizly.app.dto.ai.PdfQuizGeneratorRequest;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.exception.AiGenerationException;
import com.quizly.app.exception.AiHighDemandException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiQuizGeneratorService {

    private final GeminiApiClient geminiApiClient;
    private final AiContextPlanner contextPlanner;
    private final AiQuizResponseParser responseParser;

    public AiQuizGeneratorService(
            GeminiApiClient geminiApiClient,
            AiContextPlanner contextPlanner,
            AiQuizResponseParser responseParser) {
        this.geminiApiClient = geminiApiClient;
        this.contextPlanner = contextPlanner;
        this.responseParser = responseParser;
    }

    public QuizForm generateQuizFromText(String extractedText, PdfQuizGeneratorRequest request, String filename) {
        try {
            List<String> pages = contextPlanner.splitPages(extractedText);
            String systemInstruction = getSystemInstruction();
            String questionType = responseParser.getRequestedQuestionType(request);
            String fullPrompt = contextPlanner.buildFullPrompt(pages, request, questionType);

            if (contextPlanner.fits(systemInstruction, fullPrompt)) {
                String response = geminiApiClient.generate(
                        systemInstruction, fullPrompt, contextPlanner.finalOutputTokens());
                return responseParser.parseQuiz(response, filename, request);
            }

            log.info("El texto excede el contexto de una llamada; se procesarán {} páginas por fragmentos.", pages.size());
            int inputBudget = contextPlanner.getInputTokenBudget();
            List<String> chunks = contextPlanner.splitIntoChunks(pages, request, questionType, inputBudget);
            List<String> summaries = new ArrayList<>();
            for (String chunk : chunks) {
                String response = geminiApiClient.generate(
                        contextPlanner.getChunkAnalysisInstruction(), chunk, contextPlanner.summaryOutputTokens());
                summaries.add(responseParser.parseSummary(response));
            }

            String finalPrompt = consolidateSummaries(summaries, request, questionType);
            String response = geminiApiClient.generate(
                    systemInstruction, finalPrompt, contextPlanner.finalOutputTokens());
            return responseParser.parseQuiz(response, filename, request);
        } catch (RestClientException e) {
            log.error("Error del proveedor IA: {}", e.getMessage());

            if (isHighDemandOrTimeout(e)) {
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

    private boolean isHighDemandOrTimeout(RestClientException e) {
        if (e instanceof RestClientResponseException respEx) {
            int statusCode = respEx.getStatusCode().value();
            String responseBody = respEx.getResponseBodyAsString() != null ? respEx.getResponseBodyAsString() : "";
            if (statusCode == 429 || statusCode == 503
                    || responseBody.contains("RESOURCE_EXHAUSTED") || responseBody.contains("UNAVAILABLE")) {
                return true;
            }
        }
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

    private String consolidateSummaries(
            List<String> summaries, PdfQuizGeneratorRequest request, String questionType) {
        List<String> current = summaries;
        while (true) {
            String finalPrompt = contextPlanner.buildFinalPrompt(current, request, questionType);
            if (contextPlanner.fits(getSystemInstruction(), finalPrompt)) {
                return finalPrompt;
            }

            List<String> reduced = new ArrayList<>();
            int start = 0;
            while (start < current.size()) {
                int bestEnd = start;
                int low = start + 1;
                int high = current.size();
                while (low <= high) {
                    int middle = low + (high - low) / 2;
                    String candidate = contextPlanner.buildReductionPrompt(current, start, middle);
                    if (contextPlanner.fits(contextPlanner.getSummaryReductionInstruction(), candidate)) {
                        bestEnd = middle;
                        low = middle + 1;
                    } else {
                        high = middle - 1;
                    }
                }

                if (bestEnd == start) {
                    throw new AiGenerationException("No se pudieron consolidar los fragmentos dentro del contexto disponible.");
                }

                String response = geminiApiClient.generate(
                        contextPlanner.getSummaryReductionInstruction(),
                        contextPlanner.buildReductionPrompt(current, start, bestEnd),
                        contextPlanner.summaryOutputTokens());
                reduced.add(responseParser.parseSummary(response));
                start = bestEnd;
            }

            if (reduced.size() >= current.size()
                    && contextPlanner.totalSummaryLength(reduced) >= contextPlanner.totalSummaryLength(current)) {
                throw new AiGenerationException("El documento genera demasiados fragmentos para consolidarlos.");
            }
            current = reduced;
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
                5. Proporciona una retroalimentación concisa (máximo 1 o 2 frases) para cada opción.
                6. Responde ÚNICAMENTE con el objeto JSON válido. No incluyas bloques markdown ni explicaciones adicionales fuera del JSON.
                """;
    }
}