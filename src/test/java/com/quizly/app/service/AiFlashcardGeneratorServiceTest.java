package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import com.quizly.app.dto.ai.MazoGenerado;
import com.quizly.app.dto.ai.TarjetaGenerada;
import com.quizly.app.exception.AiGenerationException;
import com.quizly.app.exception.AiHighDemandException;
import com.quizly.app.exception.PdfProcessingException;

@ExtendWith(MockitoExtension.class)
class AiFlashcardGeneratorServiceTest {

    @Mock
    private GeminiApiClient geminiApiClient;
    @Mock
    private AiContextPlanner contextPlanner;
    @Mock
    private AiQuizResponseParser responseParser;

    @InjectMocks
    private AiFlashcardGeneratorService service;

    private final MazoGenerado mazo = new MazoGenerado("T", List.of(new TarjetaGenerada("a", "b")));

    private void texto(String contenido) {
        when(contextPlanner.splitPages(contenido)).thenReturn(List.of(contenido));
    }

    @Test
    @DisplayName("Con texto que cabe: una sola llamada a la IA y se parsea la respuesta")
    void generaConUnaSolaLlamada() {
        texto("apuntes");
        when(contextPlanner.fits(anyString(), anyString())).thenReturn(true);
        when(contextPlanner.finalOutputTokens()).thenReturn(1000);
        when(geminiApiClient.generate(anyString(), anyString(), eq(1000))).thenReturn("raw");
        when(responseParser.parseFlashcards("raw", "x.pdf")).thenReturn(mazo);

        assertSame(mazo, service.generateFlashcardsFromText("apuntes", "x.pdf", 15));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(geminiApiClient).generate(anyString(), prompt.capture(), anyInt());
        assertTrue(prompt.getValue().contains("Genera 15 tarjetas"));
        assertTrue(prompt.getValue().contains("apuntes"));
    }

    @Test
    @DisplayName("El número de tarjetas pedido se acota al máximo permitido")
    void acotaElNumeroDeTarjetas() {
        texto("apuntes");
        when(contextPlanner.fits(anyString(), anyString())).thenReturn(true);
        when(geminiApiClient.generate(anyString(), anyString(), anyInt())).thenReturn("raw");
        when(responseParser.parseFlashcards("raw", "x.pdf")).thenReturn(mazo);

        service.generateFlashcardsFromText("apuntes", "x.pdf", 500);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(geminiApiClient).generate(anyString(), prompt.capture(), anyInt());
        assertTrue(prompt.getValue().contains("Genera " + AiQuizResponseParser.MAX_TARJETAS + " tarjetas"));
    }

    @Test
    @DisplayName("Si el texto no cabe en el contexto se pide un PDF más corto y no se llama a la IA")
    void textoDemasiadoLargo() {
        texto("apuntes");
        when(contextPlanner.fits(anyString(), anyString())).thenReturn(false);

        assertThrows(PdfProcessingException.class, () -> service.generateFlashcardsFromText("apuntes", "x.pdf", 10));
        verify(geminiApiClient, never()).generate(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Un 503 del proveedor se traduce en alta demanda")
    void altaDemanda() {
        texto("apuntes");
        when(contextPlanner.fits(anyString(), anyString())).thenReturn(true);
        when(geminiApiClient.generate(anyString(), anyString(), anyInt()))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(AiHighDemandException.class, () -> service.generateFlashcardsFromText("apuntes", "x.pdf", 10));
    }

    @Test
    @DisplayName("Un error inesperado se traduce en AiGenerationException")
    void errorInesperado() {
        texto("apuntes");
        when(contextPlanner.fits(anyString(), anyString())).thenThrow(new IllegalStateException("boom"));

        assertThrows(AiGenerationException.class, () -> service.generateFlashcardsFromText("apuntes", "x.pdf", 10));
    }
}
