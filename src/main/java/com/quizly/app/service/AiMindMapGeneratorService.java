package com.quizly.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.quizly.app.dto.ai.MapaGenerado;
import com.quizly.app.exception.AiGenerationException;
import com.quizly.app.exception.AiHighDemandException;
import com.quizly.app.exception.PdfProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Genera un mapa mental a partir del texto de un PDF con una única llamada a la IA.
 * shortcut: sin troceado de documentos largos, igual que {@link AiFlashcardGeneratorService}; si el texto no cabe
 * en el contexto se pide un PDF más corto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMindMapGeneratorService {

    private final GeminiApiClient geminiApiClient;
    private final AiContextPlanner contextPlanner;
    private final AiQuizResponseParser responseParser;

    public MapaGenerado generateMindMapFromText(String extractedText, String filename) {
        try {
            List<String> pages = contextPlanner.splitPages(extractedText);
            String systemInstruction = getSystemInstruction();
            String prompt = buildPrompt(pages);

            if (!contextPlanner.fits(systemInstruction, prompt)) {
                throw new PdfProcessingException(
                        "El PDF es demasiado largo para generar un mapa mental. Prueba con un documento más corto.");
            }

            String response = geminiApiClient.generate(systemInstruction, prompt, contextPlanner.finalOutputTokens());
            return responseParser.parseMindMap(response, filename);
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

    private String buildPrompt(List<String> pages) {
        return "Genera un mapa mental a partir del siguiente documento.\n"
                + "\nTEXTO FUENTE EXTRAÍDO DEL DOCUMENTO:\n" + String.join("\n", pages);
    }

    private String getSystemInstruction() {
        return """
                Eres un experto pedagogo que resume documentos en mapas mentales para estudiar.
                Tu tarea es generar un mapa mental claro y jerárquico en formato JSON estricto a partir del texto proporcionado.

                Estructura JSON obligatoria requerida:
                {
                    "titulo": "Título representativo y conciso del mapa",
                    "raiz": {
                        "texto": "Idea central del documento",
                        "hijos": [
                            {
                                "texto": "Bloque principal",
                                "hijos": [
                                    { "texto": "Concepto o detalle clave" }
                                ]
                            }
                        ]
                    }
                }

                REGLAS ESTRICTAS:
                1. Máximo 4 niveles contando la raíz y unos 60 nodos en total.
                2. Cada nodo es una idea corta (máximo 8 palabras y nunca más de 100 caracteres): nada de frases largas ni párrafos.
                3. La raíz es la idea central; sus hijos, los grandes bloques del documento; los niveles inferiores, los conceptos y detalles clave.
                4. Todo el contenido debe estar redactado en español y basarse únicamente en la información del texto proporcionado.
                5. No repitas ideas entre nodos ni numeres los textos.
                6. Responde ÚNICAMENTE con el objeto JSON válido. No incluyas bloques markdown ni explicaciones adicionales fuera del JSON.
                """;
    }
}
