package com.quizly.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.dto.ai.PdfQuizGeneratorRequest;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.dto.form.QuizFormOptions;
import com.quizly.app.repository.DificultadRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.service.AiQuizGeneratorService;
import com.quizly.app.service.BreadcrumbService;
import com.quizly.app.service.CuestionarioService;
import com.quizly.app.service.FavoritoService;
import com.quizly.app.service.PdfExtractorService;
import com.quizly.app.service.QuizFormService;
import com.quizly.app.web.model.BreadcrumbContext;
import com.quizly.app.web.notification.ToastService;

class MisCuestionariosControllerTest {

    @Test
    void generarQuizIaSolicitaSoloPreguntasVerdaderoFalso() {
        CuestionarioService cuestionarioService = mock(CuestionarioService.class);
        FavoritoService favoritoService = mock(FavoritoService.class);
        QuizFormService quizFormService = mock(QuizFormService.class);
        PdfExtractorService pdfExtractorService = mock(PdfExtractorService.class);
        AiQuizGeneratorService aiQuizGeneratorService = mock(AiQuizGeneratorService.class);
        DificultadRepository dificultadRepository = mock(DificultadRepository.class);
        TemaRepository temaRepository = mock(TemaRepository.class);
        ToastService toastService = mock(ToastService.class);
        BreadcrumbContext breadcrumbContext = new BreadcrumbContext();
        BreadcrumbService breadcrumbService = mock(BreadcrumbService.class);

        MisCuestionariosController controller = new MisCuestionariosController(
                cuestionarioService,
                favoritoService,
                quizFormService,
                pdfExtractorService,
                aiQuizGeneratorService,
                dificultadRepository,
                temaRepository,
                toastService,
                breadcrumbContext,
                breadcrumbService);

        MockMultipartFile file = new MockMultipartFile("file", "apuntes.pdf", "application/pdf", new byte[] { 1 });
        when(pdfExtractorService.extractTextFromPdf(file)).thenReturn("Texto de los apuntes");
        when(dificultadRepository.findByCodigo("MEDIUM")).thenReturn(Optional.empty());
        when(aiQuizGeneratorService.generateQuizFromText(any(), any(), any())).thenReturn(new QuizForm());
        when(quizFormService.getOptions(null)).thenReturn(new QuizFormOptions(
                List.of(), List.of(), List.of(), List.of()));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.generarQuizIa(
                file,
                10,
                "MEDIUM",
                "TRUE_FALSE",
                model,
                mock(RedirectAttributes.class));

        ArgumentCaptor<PdfQuizGeneratorRequest> requestCaptor = ArgumentCaptor.forClass(PdfQuizGeneratorRequest.class);
        verify(aiQuizGeneratorService).generateQuizFromText(
                eq("Texto de los apuntes"), requestCaptor.capture(), eq("apuntes.pdf"));

        assertEquals("admin/cuestionarios/quiz-form", view);
        assertEquals(false, model.getAttribute("canImportQuizJson"));
        assertTrue(requestCaptor.getValue().getIndicacionesAdicionales()
                .contains("únicamente preguntas de verdadero/falso (TRUE_FALSE)"));
        assertTrue(requestCaptor.getValue().getIndicacionesAdicionales().contains("'Verdadero' y 'Falso'"));
    }
}