package com.quizly.app.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.quizly.app.exception.PdfProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
public class PdfExtractorService {
    private final long maxFileSizeBytes;

    public PdfExtractorService(@Value("${ai.max-file-size-mb:20}") int maxFileSizeMb){
        this.maxFileSizeBytes = maxFileSizeMb * 1024L * 1024L;
    }

    public String extractTextFromPdf(MultipartFile file){
        validatePdfFile(file);

        try (InputStream is = file.getInputStream()) {
            // Validar cabecera mágica
            byte[] header = new byte[5];
            int bytesRead = is.read(header);

            if (bytesRead < 5 || !new String(header, StandardCharsets.US_ASCII).startsWith("%PDF-")) {
                throw new PdfProcessingException("El archivo subido no es un PDF válido.");
            }
        } catch (PdfProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al leer los bytes del archivo PDF", e);
            throw new PdfProcessingException("No se pudo leer el archivo por proporcionado.");
        }

        try (InputStream is = file.getInputStream();
            PDDocument document = Loader.loadPDF(is.readAllBytes())) {

            if (document.isEncrypted()) {
                throw new PdfProcessingException("El archivo PDF está protegido y no se puede procesar.");
            }

            int totalPages = document.getNumberOfPages();

            if (totalPages == 0) {
                throw new PdfProcessingException("El archivo PDF no contiene páginas.");
            }

            log.info("Procesando PDF '{}' de '{}' páginas.", file.getOriginalFilename(), totalPages);

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setPageEnd("\f");

            String text = textStripper.getText(document).trim();

            // Detección de PDF escaneado o sin texto reconocible
            String recognizableText = text.replace("\f", "").trim();
            if (recognizableText.length() < 100) {
                throw new PdfProcessingException("El PDF no contiene texto suficiente o es un documento escaneado (imagen). " 
                + "Por favor, sube un PDF con texto reconocible.");
            }

            // Sanitización básica de caracteres de control
            String sanitizedText = text.replaceAll("[\\x00-\\x08\\x0B\\x0E-\\x1F]", "");

            log.info("Texto extraído con éxito ({}) caracteres.", sanitizedText.length());
            return sanitizedText;

        } catch (PdfProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al procesar el archivo '{}'", file.getOriginalFilename());
            throw new PdfProcessingException("Error al extraer el contenido del PDF: " + e.getMessage(), e);
        }
    }

    private void validatePdfFile(MultipartFile file){
        if (file == null || file.isEmpty()) {
            throw new PdfProcessingException("Por favor, selecciona un archivo PDF.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new PdfProcessingException(
                "El archivo supera el tamaño permitido (" + (maxFileSizeBytes / (1024 * 1024)) + " MB)."
            );
        }
    }
}
