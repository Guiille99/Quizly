package com.quizly.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.dto.ai.PdfQuizGeneratorRequest;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.dto.form.QuizFormOptions;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.repository.DificultadRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.security.RateLimiter;
import com.quizly.app.service.AiQuizGeneratorService;
import com.quizly.app.service.BreadcrumbService;
import com.quizly.app.service.CuestionarioService;
import com.quizly.app.service.FavoritoService;
import com.quizly.app.service.PdfExtractorService;
import com.quizly.app.service.QuizFormService;
import com.quizly.app.web.model.BreadcrumbContext;
import com.quizly.app.web.notification.ToastService;

class MisCuestionariosControllerTest {

    private PdfExtractorService pdfExtractorService;
    private AiQuizGeneratorService aiQuizGeneratorService;
    private DificultadRepository dificultadRepository;
    private QuizFormService quizFormService;
    private ToastService toastService;
    private MockMultipartFile file;
    private QuizlyUserDetails authUser;
    private RedirectAttributes ra;
    private MisCuestionariosController controller;

    @BeforeEach
    void setUp() {
        CuestionarioService cuestionarioService = mock(CuestionarioService.class);
        com.quizly.app.service.CuestionarioCompartidoService cuestionarioCompartidoService = mock(com.quizly.app.service.CuestionarioCompartidoService.class);
        FavoritoService favoritoService = mock(FavoritoService.class);
        quizFormService = mock(QuizFormService.class);
        pdfExtractorService = mock(PdfExtractorService.class);
        aiQuizGeneratorService = mock(AiQuizGeneratorService.class);
        dificultadRepository = mock(DificultadRepository.class);
        TemaRepository temaRepository = mock(TemaRepository.class);
        toastService = mock(ToastService.class);
        BreadcrumbContext breadcrumbContext = new BreadcrumbContext();
        BreadcrumbService breadcrumbService = mock(BreadcrumbService.class);

        controller = new MisCuestionariosController(
                cuestionarioService,
                cuestionarioCompartidoService,
                favoritoService,
                quizFormService,
                pdfExtractorService,
                aiQuizGeneratorService,
                dificultadRepository,
                temaRepository,
                toastService,
                breadcrumbContext,
                breadcrumbService,
                new RateLimiter());

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        authUser = new QuizlyUserDetails(usuario);
        ra = mock(RedirectAttributes.class);
        file = new MockMultipartFile("file", "apuntes.pdf", "application/pdf", new byte[] { 1 });

        when(pdfExtractorService.extractTextFromPdf(file)).thenReturn("Texto de los apuntes");
        when(dificultadRepository.findByCodigo("MEDIUM")).thenReturn(Optional.empty());
        when(aiQuizGeneratorService.generateQuizFromText(any(), any(), any())).thenReturn(new QuizForm());
        when(quizFormService.getOptions(null)).thenReturn(new QuizFormOptions(
                List.of(), List.of(), List.of(), List.of()));
    }

    private String generar() {
        return controller.generarQuizIa(file, 10, "MEDIUM", "VARIADAS", authUser, new ExtendedModelMap(), ra);
    }

    @Test
    void generarQuizIaSolicitaSoloPreguntasVerdaderoFalso() {
        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.generarQuizIa(
                file,
                10,
                "MEDIUM",
                "TRUE_FALSE",
                authUser,
                model,
                mock(RedirectAttributes.class));

        ArgumentCaptor<PdfQuizGeneratorRequest> requestCaptor = ArgumentCaptor.forClass(PdfQuizGeneratorRequest.class);
        verify(aiQuizGeneratorService).generateQuizFromText(
                eq("Texto de los apuntes"), requestCaptor.capture(), eq("apuntes.pdf"));

        assertEquals("admin/cuestionarios/quiz-form", view);
        assertEquals(false, model.getAttribute("canImportQuizJson"));
        assertEquals("TRUE_FALSE", requestCaptor.getValue().getTipoPreguntas());
        assertTrue(requestCaptor.getValue().getIndicacionesAdicionales()
                .contains("únicamente preguntas de verdadero/falso (TRUE_FALSE)"));
        assertTrue(requestCaptor.getValue().getIndicacionesAdicionales().contains("'Verdadero' y 'Falso'"));
    }

    @Test
    void generarQuizIaRechazaAlSuperarElLimitePorHora() {
        for (int i = 0; i < 10; i++) {
            assertEquals("admin/cuestionarios/quiz-form", generar());
        }

        assertEquals("redirect:/mis-cuestionarios", generar());

        verify(aiQuizGeneratorService, times(10)).generateQuizFromText(any(), any(), any());
        verify(toastService).error(eq(ra),
                eq("Has alcanzado el límite de generaciones con IA. Inténtalo de nuevo más tarde."));
    }

    @Test
    void generarQuizIaRechazaLaTerceraGeneracionSimultanea() {
        AtomicInteger profundidad = new AtomicInteger();
        List<String> vistasAnidadas = new ArrayList<>();
        // Cada llamada a la IA lanza otra generación mientras la anterior sigue en curso
        when(aiQuizGeneratorService.generateQuizFromText(any(), any(), any())).thenAnswer(inv -> {
            if (profundidad.incrementAndGet() <= 2) {
                vistasAnidadas.add(generar());
            }
            return new QuizForm();
        });

        String vistaExterna = generar();

        assertEquals("admin/cuestionarios/quiz-form", vistaExterna);
        assertEquals(List.of("redirect:/mis-cuestionarios", "admin/cuestionarios/quiz-form"), vistasAnidadas);
        verify(aiQuizGeneratorService, times(2)).generateQuizFromText(any(), any(), any());
        verify(toastService).error(eq(ra),
                eq("Hay demasiadas generaciones en curso. Inténtalo de nuevo en unos minutos."));

        // Al terminar se liberan los huecos
        assertEquals("admin/cuestionarios/quiz-form", generar());
    }
}
