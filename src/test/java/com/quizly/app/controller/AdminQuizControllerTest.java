package com.quizly.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.dto.form.QuizFormOptions;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.service.AdminQuizService;
import com.quizly.app.service.BreadcrumbService;
import com.quizly.app.service.CuestionarioService;
import com.quizly.app.service.QuizFormService;
import com.quizly.app.service.QuizJsonImportService;
import com.quizly.app.web.model.BreadcrumbContext;
import com.quizly.app.web.notification.ToastService;

class AdminQuizControllerTest {
    private AdminQuizController controller;
    private QuizJsonImportService quizJsonImportService;
    private QuizFormService quizFormService;
    private CuestionarioService cuestionarioService;
    private ToastService toastService;

    @BeforeEach
    void setUp() {
        quizJsonImportService = mock(QuizJsonImportService.class);
        quizFormService = mock(QuizFormService.class);
        cuestionarioService = mock(CuestionarioService.class);
        toastService = mock(ToastService.class);

        controller = new AdminQuizController(
                mock(AsignaturaRepository.class),
                mock(TemaRepository.class),
                mock(CuestionarioRepository.class),
                mock(AdminQuizService.class),
                quizFormService,
                cuestionarioService,
                quizJsonImportService,
                toastService,
                new BreadcrumbContext(),
                mock(BreadcrumbService.class));

        when(quizFormService.getOptions(null)).thenReturn(new QuizFormOptions(
                List.of(), List.of(), List.of(), List.of()));
    }

    @Test
    void importarJsonPreparaVistaSinGuardarCuestionario() {
        QuizForm form = new QuizForm();
        MockMultipartFile file = new MockMultipartFile("jsonFile", "quiz.json", "application/json", "{}".getBytes());
        when(quizJsonImportService.importIntoForm(file, form)).thenReturn(form);
        ExtendedModelMap model = new ExtendedModelMap();
        model.addAttribute("quizForm", form);

        String view = controller.importQuizJson(file, form, model);

        assertEquals("admin/cuestionarios/quiz-form", view);
        assertEquals(form, model.getAttribute("quizForm"));
        assertEquals("/admin/cuestionarios/nuevo", model.getAttribute("formAction"));
        assertTrue((Boolean) model.getAttribute("canImportQuizJson"));
        verify(quizJsonImportService).importIntoForm(file, form);
        verify(cuestionarioService, never()).createQuiz(any());
        verify(toastService).success(model, "JSON cargado. Revisa y completa el formulario antes de guardar.");
    }

    @Test
    void importarJsonMuestraErrorSinPerderFormulario() {
        QuizForm form = new QuizForm();
        form.setTitulo("Título escrito antes de importar");
        MockMultipartFile file = new MockMultipartFile("jsonFile", "quiz.json", "application/json", "{}".getBytes());
        ExtendedModelMap model = new ExtendedModelMap();
        when(quizJsonImportService.importIntoForm(file, form))
                .thenThrow(new IllegalArgumentException("El archivo no contiene un JSON válido."));

        String view = controller.importQuizJson(file, form, model);

        assertEquals("admin/cuestionarios/quiz-form", view);
        assertEquals("Título escrito antes de importar", form.getTitulo());
        assertEquals("/admin/cuestionarios/nuevo", model.getAttribute("formAction"));
        verify(toastService).error(model, "El archivo no contiene un JSON válido.");
        verify(cuestionarioService, never()).createQuiz(any());
    }

    @Test
    void importarJsonRechazaFormularioDeEdicion() {
        QuizForm form = new QuizForm();
        form.setId(9L);
        MockMultipartFile file = new MockMultipartFile("jsonFile", "quiz.json", "application/json", "{}".getBytes());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.importQuizJson(file, form, model);

        assertEquals("admin/cuestionarios/quiz-form", view);
        verify(quizJsonImportService, never()).importIntoForm(any(), any());
        verify(toastService).error(model, "La importación JSON solo está disponible al crear un cuestionario.");
        verify(cuestionarioService, never()).createQuiz(any());
    }
}