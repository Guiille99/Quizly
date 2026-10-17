package com.quizly.app.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.quizly.app.dto.ai.PdfQuizGeneratorRequest;
import com.quizly.app.exception.AiGenerationException;

@Component
public class AiContextPlanner {

    private final GeminiApiClient geminiApiClient;
    private final int contextWindowTokens;
    private final int outputTokenReserve;
    private final double contextSafetyMargin;

    public AiContextPlanner(
            GeminiApiClient geminiApiClient,
            @Value("${ai.context-window-tokens:1000000}") int contextWindowTokens,
            @Value("${ai.output-token-reserve:8192}") int outputTokenReserve,
            @Value("${ai.context-safety-margin:0.02}") double contextSafetyMargin) {
        this.geminiApiClient = geminiApiClient;
        this.contextWindowTokens = contextWindowTokens;
        this.outputTokenReserve = outputTokenReserve;
        this.contextSafetyMargin = contextSafetyMargin;
    }

    int getInputTokenBudget() {
        int usableTokens = Math.max(1, contextWindowTokens - outputTokenReserve);
        double safetyFactor = Math.max(0.0, Math.min(contextSafetyMargin, 0.5));
        return Math.max(1, (int) Math.floor(usableTokens * (1.0 - safetyFactor)));
    }

    boolean fits(String systemInstruction, String prompt) {
        long maximumCounterRequestChars = Math.max(1L, (long) contextWindowTokens * 4);
        if ((long) systemInstruction.length() + prompt.length() > maximumCounterRequestChars) {
            return false;
        }
        return geminiApiClient.countTokens(systemInstruction, prompt) <= getInputTokenBudget();
    }

    List<String> splitPages(String text) {
        List<String> pages = new ArrayList<>(List.of(text.split("\\f", -1)));
        while (pages.size() > 1 && pages.get(pages.size() - 1).isBlank()) {
            pages.remove(pages.size() - 1);
        }
        return pages;
    }

    String buildFullPrompt(List<String> pages, PdfQuizGeneratorRequest request, String questionType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Por favor, genera un cuestionario con las siguientes especificaciones:\n");
        sb.append("- Número de preguntas: ").append(request.getNumPreguntas()).append("\n");
        sb.append("- Tipo de preguntas: ").append(questionType).append("\n");
        appendAdditionalInstructions(sb, request);
        sb.append("\nTEXTO FUENTE EXTRAÍDO DEL DOCUMENTO:\n");
        appendPages(sb, pages, 0, pages.size());
        return sb.toString();
    }

    String getChunkAnalysisInstruction() {
        return """
                Analiza todo el fragmento de documento y prepara información útil para generar un cuestionario.
                No generes todavía el cuestionario final ni inventes información.
                Devuelve únicamente JSON con las propiedades "resumen", "conceptosClave" y "hechosPreguntables".
                El resumen debe ser conciso; conserva definiciones, relaciones, datos y excepciones relevantes.
                """;
    }

    String getSummaryReductionInstruction() {
        return """
                Integra las notas de varios fragmentos de un mismo documento sin perder hechos importantes.
                Elimina duplicados, conserva relaciones y excepciones, y no añadas información nueva.
                Devuelve únicamente JSON con las propiedades "resumen", "conceptosClave" y "hechosPreguntables".
                """;
    }

    int summaryOutputTokens() {
        return Math.min(outputTokenReserve, 2048);
    }

    int finalOutputTokens() {
        return outputTokenReserve;
    }

    List<String> splitIntoChunks(
            List<String> pages, PdfQuizGeneratorRequest request, String questionType, int inputBudget) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < pages.size()) {
            int low = start + 1;
            int high = pages.size();
            int bestEnd = start;

            while (low <= high) {
                int middle = low + (high - low) / 2;
                String candidate = buildChunkPrompt(pages, start, middle, request, questionType);
                if (countTokens(getChunkAnalysisInstruction(), candidate) <= inputBudget) {
                    bestEnd = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }

            if (bestEnd == start) {
                splitOversizedPage(pages.get(start), start + 1, request, questionType, inputBudget, chunks);
                start++;
            } else {
                chunks.add(buildChunkPrompt(pages, start, bestEnd, request, questionType));
                start = bestEnd;
            }
        }

        return chunks;
    }

    private void splitOversizedPage(
            String pageText,
            int pageNumber,
            PdfQuizGeneratorRequest request,
            String questionType,
            int inputBudget,
            List<String> chunks) {
        String prompt = buildSinglePageChunkPrompt(pageText, pageNumber, request, questionType);
        if (countTokens(getChunkAnalysisInstruction(), prompt) <= inputBudget) {
            chunks.add(prompt);
            return;
        }

        if (pageText.length() < 2) {
            throw new AiGenerationException("Una página supera el contexto máximo configurado y no se puede fragmentar.");
        }

        int splitAt = pageText.lastIndexOf(' ', pageText.length() / 2);
        if (splitAt < 1 || splitAt >= pageText.length() - 1) {
            splitAt = pageText.length() / 2;
        }

        splitOversizedPage(pageText.substring(0, splitAt + 1), pageNumber, request, questionType, inputBudget, chunks);
        splitOversizedPage(pageText.substring(splitAt + 1), pageNumber, request, questionType, inputBudget, chunks);
    }

    private String buildChunkPrompt(
            List<String> pages, int start, int end, PdfQuizGeneratorRequest request, String questionType) {
        StringBuilder sb = new StringBuilder();
        appendChunkInstructions(sb, request, questionType);
        sb.append("\nFRAGMENTO DEL DOCUMENTO:\n");
        appendPages(sb, pages, start, end);
        return sb.toString();
    }

    private String buildSinglePageChunkPrompt(
            String pageText, int pageNumber, PdfQuizGeneratorRequest request, String questionType) {
        StringBuilder sb = new StringBuilder();
        appendChunkInstructions(sb, request, questionType);
        sb.append("\nFRAGMENTO DEL DOCUMENTO:\n[PÁGINA ").append(pageNumber).append("]\n").append(pageText);
        return sb.toString();
    }

    private void appendChunkInstructions(StringBuilder sb, PdfQuizGeneratorRequest request, String questionType) {
        sb.append("Analiza este fragmento como parte del documento completo. Tipo de cuestionario solicitado: ")
                .append(questionType).append(".\n");
        appendAdditionalInstructions(sb, request);
    }

    private void appendAdditionalInstructions(StringBuilder sb, PdfQuizGeneratorRequest request) {
        if (StringUtils.isNotBlank(request.getIndicacionesAdicionales())) {
            sb.append("- Enfoque/Indicaciones especiales: ")
                    .append(request.getIndicacionesAdicionales().trim()).append("\n");
        }
    }

    private void appendPages(StringBuilder sb, List<String> pages, int start, int end) {
        for (int pageIndex = start; pageIndex < end; pageIndex++) {
            sb.append("\n[PÁGINA ").append(pageIndex + 1).append("]\n").append(pages.get(pageIndex));
        }
    }

    String buildFinalPrompt(List<String> summaries, PdfQuizGeneratorRequest request, String questionType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Genera el cuestionario solicitado usando todas las notas de cobertura del documento.\n");
        sb.append("- Número de preguntas: ").append(request.getNumPreguntas()).append("\n");
        sb.append("- Tipo de preguntas: ").append(questionType).append("\n");
        appendAdditionalInstructions(sb, request);
        sb.append("\nNOTAS DE TODO EL DOCUMENTO:\n");
        summaries.forEach(summary -> sb.append("\n").append(summary));
        return sb.toString();
    }

    String buildReductionPrompt(List<String> summaries, int start, int end) {
        StringBuilder sb = new StringBuilder("Integra estas notas de contenido del documento:\n");
        for (int index = start; index < end; index++) {
            sb.append("\nNOTAS ").append(index + 1).append(":\n").append(summaries.get(index));
        }
        return sb.toString();
    }

    int totalSummaryLength(List<String> summaries) {
        return summaries.stream().mapToInt(String::length).sum();
    }

    private int countTokens(String systemInstruction, String prompt) {
        long maximumCounterRequestChars = Math.max(1L, (long) contextWindowTokens * 4);
        if ((long) systemInstruction.length() + prompt.length() > maximumCounterRequestChars) {
            return Integer.MAX_VALUE;
        }
        return geminiApiClient.countTokens(systemInstruction, prompt);
    }
}