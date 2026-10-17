package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.quizly.app.exception.PdfProcessingException;

class PdfExtractorServiceTest {

    @Test
    void extraeTextoDePaginasPosterioresALimiteAnteriorDeTreinta() throws Exception {
        byte[] pdfBytes;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int pageNumber = 1; pageNumber <= 31; pageNumber++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(50, 700);
                    content.showText("Pagina " + pageNumber + " contenido suficiente para validar la extraccion completa.");
                    content.endText();
                }
            }
            document.save(output);
            pdfBytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "31-paginas.pdf", "application/pdf", pdfBytes);
        PdfExtractorService service = new PdfExtractorService(20);

        String extractedText = service.extractTextFromPdf(file);

        assertTrue(extractedText.contains("Pagina 1"));
        assertTrue(extractedText.contains("Pagina 15"));
        assertTrue(extractedText.contains("Pagina 31"));
        assertTrue(extractedText.contains("\f"));
        assertTrue(extractedText.indexOf("Pagina 1") < extractedText.indexOf("Pagina 15"));
        assertTrue(extractedText.indexOf("Pagina 15") < extractedText.indexOf("Pagina 31"));
    }

    @Test
    void rechazaPdfConMenosDeCienCaracteresReconociblesAunqueTengaMuchasPaginas() throws Exception {
        byte[] pdfBytes;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int pageNumber = 1; pageNumber <= 110; pageNumber++) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (pageNumber == 1) {
                    try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        content.newLineAtOffset(50, 700);
                        content.showText("texto corto");
                        content.endText();
                    }
                }
            }
            document.save(output);
            pdfBytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "poco-texto.pdf", "application/pdf", pdfBytes);
        PdfExtractorService service = new PdfExtractorService(20);

        assertThrows(PdfProcessingException.class, () -> service.extractTextFromPdf(file));
    }
}