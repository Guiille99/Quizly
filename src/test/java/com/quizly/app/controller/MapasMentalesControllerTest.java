package com.quizly.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.dto.ai.MapaGenerado;
import com.quizly.app.exception.AiHighDemandException;
import com.quizly.app.exception.PdfProcessingException;
import com.quizly.app.model.entity.MapaMental;
import com.quizly.app.model.view.mapa.ResultadoMazoDesdeMapa;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.security.RateLimiter;
import com.quizly.app.service.AiMindMapGeneratorService;
import com.quizly.app.service.BreadcrumbService;
import com.quizly.app.service.MapaMentalService;
import com.quizly.app.service.PdfExtractorService;
import com.quizly.app.web.model.BreadcrumbContext;
import com.quizly.app.web.notification.ToastService;

@ExtendWith(MockitoExtension.class)
class MapasMentalesControllerTest {

    @Mock
    private MapaMentalService mapaMentalService;
    @Mock
    private PdfExtractorService pdfExtractorService;
    @Mock
    private AiMindMapGeneratorService aiMindMapGeneratorService;
    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private BreadcrumbContext breadcrumbContext;
    @Mock
    private BreadcrumbService breadcrumbService;
    @Mock
    private ToastService toastService;

    @InjectMocks
    private MapasMentalesController controller;

    private final QuizlyUserDetails admin = mock(QuizlyUserDetails.class);
    private final RedirectAttributes ra = mock(RedirectAttributes.class);
    private final MockMultipartFile pdf = new MockMultipartFile("file", "apuntes.pdf", "application/pdf", new byte[] { 1 });

    @BeforeEach
    void setUp() {
        when(admin.getId()).thenReturn(7L);
    }

    // =========================================================================
    // Generación con IA
    // =========================================================================

    @Test
    @DisplayName("Genera el mapa, lo guarda y redirige a su editor")
    void generaYRedirige() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(pdfExtractorService.extractTextFromPdf(pdf)).thenReturn("texto");
        MapaGenerado generado = new MapaGenerado("T", "{}", 12);
        when(aiMindMapGeneratorService.generateMindMapFromText("texto", "apuntes.pdf")).thenReturn(generado);
        MapaMental mapa = new MapaMental();
        mapa.setId(42L);
        when(mapaMentalService.crearDesdeIa(7L, generado)).thenReturn(mapa);

        assertEquals("redirect:/mapas/42", controller.generarIa(pdf, admin, ra));

        verify(toastService).success(eq(ra), anyString());
    }

    @Test
    @DisplayName("Superado el límite de uso no se procesa el PDF ni se llama a la IA")
    void limiteDeUso() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        assertEquals("redirect:/mapas", controller.generarIa(pdf, admin, ra));

        verify(toastService).error(eq(ra), anyString());
        verify(pdfExtractorService, never()).extractTextFromPdf(any());
        verify(aiMindMapGeneratorService, never()).generateMindMapFromText(any(), any());
    }

    @Test
    @DisplayName("Comparte la clave de cuota con cuestionarios y tarjetas")
    void claveDeCuotaCompartida() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        controller.generarIa(pdf, admin, ra);

        verify(rateLimiter).tryAcquire(eq("ia:7:h"), anyInt(), any(Duration.class));
    }

    @Test
    @DisplayName("Un PDF inválido muestra su mensaje y no guarda nada")
    void pdfInvalido() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(pdfExtractorService.extractTextFromPdf(pdf)).thenThrow(new PdfProcessingException("PDF vacío"));

        assertEquals("redirect:/mapas", controller.generarIa(pdf, admin, ra));

        verify(toastService).error(ra, "PDF vacío");
        verify(mapaMentalService, never()).crearDesdeIa(any(), any());
    }

    @Test
    @DisplayName("Una IA saturada no guarda nada y libera el hueco para reintentar")
    void iaSaturada() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(pdfExtractorService.extractTextFromPdf(pdf)).thenReturn("texto");
        when(aiMindMapGeneratorService.generateMindMapFromText(any(), any()))
                .thenThrow(new AiHighDemandException("saturada"));

        // Tres intentos seguidos: si el semáforo no se liberara en el finally, el tercero diría "demasiadas generaciones"
        controller.generarIa(pdf, admin, ra);
        controller.generarIa(pdf, admin, ra);
        controller.generarIa(pdf, admin, ra);

        verify(mapaMentalService, never()).crearDesdeIa(any(), any());
        verify(aiMindMapGeneratorService, times(3)).generateMindMapFromText(any(), any());
    }

    // =========================================================================
    // Crear mazo desde un mapa
    // =========================================================================

    @Test
    @DisplayName("Crear mazo redirige a la edición del mazo y avisa del recorte si lo hubo")
    void crearMazoConRecorte() {
        when(mapaMentalService.crearMazo(3L, 7L)).thenReturn(new ResultadoMazoDesdeMapa(9L, 50, 61));

        assertEquals("redirect:/mazos/9/editar", controller.crearMazo(3L, admin, ra));

        verify(toastService).warning(eq(ra), org.mockito.ArgumentMatchers.contains("50 de 61"));
    }

    @Test
    @DisplayName("Crear mazo de un mapa sin ramas avisa y vuelve al editor")
    void crearMazoSinRamas() {
        when(mapaMentalService.crearMazo(3L, 7L)).thenReturn(new ResultadoMazoDesdeMapa(null, 0, 0));

        assertEquals("redirect:/mapas/3", controller.crearMazo(3L, admin, ra));

        verify(toastService).warning(eq(ra), anyString());
    }
}
