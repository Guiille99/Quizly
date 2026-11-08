package com.quizly.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.quizly.app.dto.ai.MazoGenerado;
import com.quizly.app.exception.AiGenerationException;
import com.quizly.app.exception.AiHighDemandException;
import com.quizly.app.exception.PdfProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Genera un mazo de tarjetas a partir del texto de un PDF con una única llamada a la IA.
 * shortcut: sin troceado de documentos largos (el cuestionario sí lo hace); si el texto no cabe en
 * el contexto se pide un PDF más corto. Reutilizar la consolidación de AiQuizGeneratorService si hace falta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFlashcardGeneratorService {

    private final GeminiApiClient geminiApiClient;
    private final AiContextPlanner contextPlanner;
    private final AiQuizResponseParser responseParser;

    public MazoGenerado generateFlashcardsFromText(String extractedText, String filename, int numTarjetas) {
        int solicitadas = Math.max(1, Math.min(numTarjetas, AiQuizResponseParser.MAX_TARJETAS));
        try {
            List<String> pages = contextPlanner.splitPages(extractedText);
            String systemInstruction = getSystemInstruction();
            String prompt = buildPrompt(pages, solicitadas);

            if (!contextPlanner.fits(systemInstruction, prompt)) {
                throw new PdfProcessingException(
                        "El PDF es demasiado largo para generar tarjetas. Prueba con un documento más corto.");
            }

            String response = geminiApiClient.generate(systemInstruction, prompt, contextPlanner.finalOutputTokens());
            return responseParser.parseFlashcards(response, filename);
        } catch (RestClientException e) {
            log.error("Error del proveedor IA: {}", e.getMessage());

            if (AiQuizGeneratorService.isHighDemandOrTimeout(e)) {
                throw new AiHighDemandException("El modelo de IA está experimentando una alta demanda en este momento");
            }
            throw new AiGenerationException(e.getMessage());
        } catch (AiGenerationException | PdfProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al comunicarse con el proveedor de IA", e);
            throw new AiGenerationException(e.getMessage());
        }
    }

    private String buildPrompt(List<String> pages, int numTarjetas) {
        return "Genera " + numTarjetas + " tarjetas de estudio a partir del siguiente documento.\n"
                + "\nTEXTO FUENTE EXTRAÍDO DEL DOCUMENTO:\n" + String.join("\n", pages);
    }

    private String getSystemInstruction() {
        return """
                Eres un experto pedagogo que prepara tarjetas de estudio (flashcards) para memorizar y repasar.
                Tu tarea es generar tarjetas de alta calidad en formato JSON estricto a partir del texto proporcionado.

                Estructura JSON obligatoria requerida:
                {
                    "titulo": "Título representativo y conciso del mazo",
                    "tarjetas": [
                        {
                            "anverso": "Pregunta, concepto o término a recordar",
                            "reverso": "Respuesta o definición clara y concisa"
                        }
                    ]
                }

                REGLAS ESTRICTAS:
                1. Genera exactamente el número de tarjetas solicitado.
                2. Cada tarjeta trata un único concepto. El anverso no supera los 200 caracteres y el reverso los 400.
                3. El anverso debe poder entenderse sin el resto de tarjetas y no debe contener la respuesta.
                4. Todo el contenido debe estar redactado en español y basarse únicamente en la información del texto proporcionado.
                5. No repitas conceptos entre tarjetas.
                6. Responde ÚNICAMENTE con el objeto JSON válido. No incluyas bloques markdown ni explicaciones adicionales fuera del JSON.
                """;
    }
}
