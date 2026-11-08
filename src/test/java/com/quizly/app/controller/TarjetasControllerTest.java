package com.quizly.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.dto.ai.MazoGenerado;
import com.quizly.app.dto.ai.TarjetaGenerada;
import com.quizly.app.exception.AiHighDemandException;
import com.quizly.app.exception.PdfProcessingException;
import com.quizly.app.model.entity.Mazo;
import com.quizly.app.model.view.tarjeta.ResultadoImportacion;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.security.RateLimiter;
import com.quizly.app.service.AiFlashcardGeneratorService;
import com.quizly.app.service.BreadcrumbService;
import com.quizly.app.service.MazoService;
import com.quizly.app.service.PdfExtractorService;
import com.quizly.app.web.model.BreadcrumbContext;
import com.quizly.app.web.notification.ToastService;

@ExtendWith(MockitoExtension.class)
class TarjetasControllerTest {

    @Mock
    private MazoService mazoService;
    @Mock
    private PdfExtractorService pdfExtractorService;
    @Mock
    private AiFlashcardGeneratorService aiFlashcardGeneratorService;
    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private BreadcrumbContext breadcrumbContext;
    @Mock
    private BreadcrumbService breadcrumbService;
    @Mock
    private ToastService toastService;

    @InjectMocks
    private TarjetasController controller;

    private final QuizlyUserDetails admin = mock(QuizlyUserDetails.class);
    private final RedirectAttributes ra = mock(RedirectAttributes.class);
    private final MockMultipartFile pdf = new MockMultipartFile("file", "apuntes.pdf", "application/pdf", new byte[] { 1 });

    @BeforeEach
    void setUp() {
        when(admin.getId()).thenReturn(7L);
    }

    @Test
    @DisplayName("Importar sin preguntas válidas avisa y vuelve al cuestionario, sin ir a ningún mazo")
    void importarSinMazo() {
        when(mazoService.importarDesdeCuestionario("quiz", 7L)).thenReturn(new ResultadoImportacion(null, 0, 2, 0));

        assertEquals("redirect:/cuestionarios/quiz", controller.importar("quiz", admin, ra));

        verify(toastService).warning(eq(ra), anyString());
    }

    @Test
    @DisplayName("Importar informa de las tarjetas añadidas y de los motivos de las omitidas")
    void importarConDetalle() {
        when(mazoService.importarDesdeCuestionario("quiz", 7L)).thenReturn(new ResultadoImportacion(9L, 3, 1, 2));

        assertEquals("redirect:/mazos/9/editar", controller.importar("quiz", admin, ra));

        verify(toastService).success(ra, "Tarjetas añadidas: 3 (2 ya existían, 1 sin respuesta correcta).");
    }

    @Test
    @DisplayName("Reimportar sin novedades lo dice sin afirmar que se omitió nada indebidamente")
    void importarSinNovedades() {
        when(mazoService.importarDesdeCuestionario("quiz", 7L)).thenReturn(new ResultadoImportacion(9L, 0, 0, 15));

        controller.importar("quiz", admin, ra);

        verify(toastService).info(ra, "No hay tarjetas nuevas (15 ya existían).");
    }

    @Test
    @DisplayName("Genera el mazo, lo guarda y redirige a su edición")
    void generaYRedirige() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(pdfExtractorService.extractTextFromPdf(pdf)).thenReturn("texto");
        MazoGenerado generado = new MazoGenerado("T", List.of(new TarjetaGenerada("a", "b")));
        when(aiFlashcardGeneratorService.generateFlashcardsFromText("texto", "apuntes.pdf", 15)).thenReturn(generado);
        Mazo mazo = new Mazo();
        mazo.setId(42L);
        when(mazoService.crearDesdeIa(7L, generado)).thenReturn(mazo);

        assertEquals("redirect:/mazos/42/editar", controller.generarIa(pdf, 15, admin, ra));
    }

    @Test
    @DisplayName("Superado el límite de uso no se procesa el PDF ni se llama a la IA")
    void limiteDeUso() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        assertEquals("redirect:/mazos", controller.generarIa(pdf, 15, admin, ra));

        verify(toastService).error(eq(ra), anyString());
        verify(pdfExtractorService, never()).extractTextFromPdf(any());
        verify(aiFlashcardGeneratorService, never()).generateFlashcardsFromText(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Comparte la clave de cuota con la generación de cuestionarios")
    void claveDeCuotaCompartida() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        controller.generarIa(pdf, 15, admin, ra);

        verify(rateLimiter).tryAcquire(eq("ia:7:h"), anyInt(), any(Duration.class));
    }

    @Test
    @DisplayName("Un PDF inválido muestra su mensaje y no guarda nada")
    void pdfInvalido() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(pdfExtractorService.extractTextFromPdf(pdf)).thenThrow(new PdfProcessingException("PDF vacío"));

        assertEquals("redirect:/mazos", controller.generarIa(pdf, 15, admin, ra));

        verify(toastService).error(ra, "PDF vacío");
        verify(mazoService, never()).crearDesdeIa(any(), any());
    }

    @Test
    @DisplayName("Una IA saturada no guarda nada y libera el hueco para reintentar")
    void iaSaturada() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(pdfExtractorService.extractTextFromPdf(pdf)).thenReturn("texto");
        when(aiFlashcardGeneratorService.generateFlashcardsFromText(any(), any(), anyInt()))
                .thenThrow(new AiHighDemandException("saturada"));

        // Dos intentos seguidos: si el semáforo no se liberara en el finally, el segundo diría "demasiadas generaciones"
        controller.generarIa(pdf, 15, admin, ra);
        controller.generarIa(pdf, 15, admin, ra);
        controller.generarIa(pdf, 15, admin, ra);

        verify(mazoService, never()).crearDesdeIa(any(), any());
        verify(aiFlashcardGeneratorService, org.mockito.Mockito.times(3))
                .generateFlashcardsFromText(any(), any(), anyInt());
    }
}
