package com.quizly.app.controller;

import com.quizly.app.dto.ai.PdfQuizGeneratorRequest;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.dto.form.QuizFormOptions;
import com.quizly.app.exception.AiGenerationException;
import com.quizly.app.exception.AiHighDemandException;
import com.quizly.app.exception.PdfProcessingException;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Dificultad;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.repository.DificultadRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.AiQuizGeneratorService;
import com.quizly.app.service.CuestionarioService;
import com.quizly.app.service.FavoritoService;
import com.quizly.app.service.PdfExtractorService;
import com.quizly.app.service.QuizFormService;
import com.quizly.app.web.notification.ToastService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/mis-cuestionarios")
public class MisCuestionariosController {

    @Value("${ai.max-file-size-mb:20}")
    private int maxFileSizeMb;

    private final CuestionarioService cuestionarioService;
    private final com.quizly.app.service.CuestionarioCompartidoService cuestionarioCompartidoService;
    private final FavoritoService favoritoService;
    private final QuizFormService quizFormService;
    private final PdfExtractorService pdfExtractorService;
    private final AiQuizGeneratorService aiQuizGeneratorService;
    private final DificultadRepository dificultadRepository;
    private final TemaRepository temaRepository;
    private final ToastService toastService;
    private final com.quizly.app.web.model.BreadcrumbContext breadcrumbContext;
    private final com.quizly.app.service.BreadcrumbService breadcrumbService;

    @ModelAttribute("maxFileSizeMb")
    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    // -------------------------------------------------------------------------
    // Vista principal y listado
    // -------------------------------------------------------------------------

    @GetMapping
    public String showMisCuestionarios(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Pageable pageable,
            Model model) {

        loadCuestionariosData(authUser.getId(), search, dificultadId, pageable, model);
        breadcrumbContext.set(breadcrumbService.misCuestionarios());
        return "cuestionarios/mis-cuestionarios";
    }

    // -------------------------------------------------------------------------
    // Fragmento de datos de la tabla (HTMX)
    // -------------------------------------------------------------------------

    @GetMapping("/tableData")
    public String getMisCuestionariosTableData(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Pageable pageable,
            Model model) {

        loadCuestionariosData(authUser.getId(), search, dificultadId, pageable, model);
        return "cuestionarios/mis-cuestionarios :: lista-cuestionarios_fragment";
    }

    // -------------------------------------------------------------------------
    // Opciones dinámicas de tema para el formulario (HTMX)
    // -------------------------------------------------------------------------

    @GetMapping("/opcionesTema")
    public String getOpcionesTemasForm(@RequestParam(required = false) Long asignaturaId, Model model) {
        List<Tema> temas = asignaturaId == null ? List.of() : temaRepository.findByAsignaturaId(asignaturaId);
        model.addAttribute("temas", temas);
        return "admin/cuestionarios/quiz-form :: temas-options";
    }

    // -------------------------------------------------------------------------
    // Generación con IA a partir de PDF
    // -------------------------------------------------------------------------

    @PostMapping("/generar-ia")
    public String generarQuizIa(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "numPreguntas", defaultValue = "10") Integer numPreguntas,
            @RequestParam(value = "dificultad", defaultValue = "MEDIUM") String dificultadParam,
            @RequestParam(value = "tipoPreguntas", defaultValue = "VARIADAS") String tipoPreguntas,
            Model model,
            RedirectAttributes ra) {

        try {
            // 1. Extraer texto del documento PDF
            String extractedText = pdfExtractorService.extractTextFromPdf(file);

            // 2. Resolver la entidad Dificultad
            Dificultad dificultad = resolveDificultad(dificultadParam);

            // 3. Preparar petición de generación
            String indicaciones;
            String tipoPreguntasNormalizado;
            if ("VARIADAS".equalsIgnoreCase(tipoPreguntas)) {
                tipoPreguntasNormalizado = "VARIADAS";
                indicaciones = "Genera una combinación equilibrada de preguntas de opción múltiple (SINGLE_CHOICE) y verdadero/falso (TRUE_FALSE).";
            } else if ("TRUE_FALSE".equalsIgnoreCase(tipoPreguntas)) {
                tipoPreguntasNormalizado = "TRUE_FALSE";
                indicaciones = "Genera únicamente preguntas de verdadero/falso (TRUE_FALSE), con exactamente dos opciones: 'Verdadero' y 'Falso'.";
            } else {
                tipoPreguntasNormalizado = "SINGLE_CHOICE";
                indicaciones = "Genera únicamente preguntas de opción múltiple con respuesta única (SINGLE_CHOICE).";
            }

            PdfQuizGeneratorRequest request = PdfQuizGeneratorRequest.builder()
                    .pdfFile(file)
                    .numPreguntas(numPreguntas)
                    .dificultadId(dificultad != null ? dificultad.getId() : null)
                    .indicacionesAdicionales(indicaciones)
                    .tipoPreguntas(tipoPreguntasNormalizado)
                    .build();

            // 4. Llamar a la IA (Gemini)
            QuizForm quizForm = aiQuizGeneratorService.generateQuizFromText(
                    extractedText, request, file.getOriginalFilename());

            if (dificultad != null) {
                quizForm.setDificultadId(dificultad.getId());
            }

            // 5. Cargar opciones del editor y renderizar quiz-form para revisión humana
            model.addAttribute("quizForm", quizForm);
            loadFormOptions(model, quizForm);
            breadcrumbContext.set(breadcrumbService.misCuestionariosNuevo());

            toastService.info(model, "Cuestionario generado con éxito. Revisa y edita las preguntas antes de guardarlo.");
            return "admin/cuestionarios/quiz-form";

        } catch (PdfProcessingException e) {
            log.warn("Fallo al procesar PDF subido por el usuario: {}", e.getMessage());
            toastService.error(ra, e.getMessage());
            return "redirect:/mis-cuestionarios";
        } catch(AiHighDemandException e){
            log.warn("El proveedor IA está saturado", e);
            toastService.error(ra, "El servicio de IA está experimentando una alta demanda en este momento. Por favor, inténtelo de nuevo más tarde.");
            return "redirect:/mis-cuestionarios";
        } catch (AiGenerationException e) {
            log.error("Error al generar cuestionario con IA: {}", e.getMessage(), e);
            toastService.error(ra, "Se produjo un error interno al generar el cuestionario con IA, inténtelo de nuevo más tarde");
            return "redirect:/mis-cuestionarios";
        } catch (Exception e) {
            log.error("Error inesperado en la generación con IA", e);
            toastService.error(ra, "Se ha producido un error inesperado al procesar el archivo.");
            return "redirect:/mis-cuestionarios";
        }
    }

    // -------------------------------------------------------------------------
    // Crear cuestionario manual
    // -------------------------------------------------------------------------

    @GetMapping("/nuevo")
    public String showCreateQuizForm(Model model) {
        QuizForm quizForm = quizFormService.createQuizForm();
        model.addAttribute("quizForm", quizForm);
        loadFormOptions(model, quizForm);
        breadcrumbContext.set(breadcrumbService.misCuestionariosNuevo());
        return "admin/cuestionarios/quiz-form";
    }

    @PostMapping("/nuevo")
    public String createPersonalQuiz(
            @Valid @ModelAttribute("quizForm") QuizForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Model model,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.misCuestionariosNuevo());
            return "admin/cuestionarios/quiz-form";
        }

        try {
            cuestionarioService.createQuiz(form, authUser.getUsuario());
            toastService.success(ra, "El cuestionario se ha creado correctamente.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            toastService.error(model, e.getMessage());
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.misCuestionariosNuevo());
            return "admin/cuestionarios/quiz-form";
        } catch (DataIntegrityViolationException e) {
            toastService.error(model, "No se pudo crear el cuestionario por un conflicto de identificador interno. Inténtalo de nuevo.");
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.misCuestionariosNuevo());
            return "admin/cuestionarios/quiz-form";
        } catch (Exception e) {
            log.error("Error al crear cuestionario personal para usuario {}", authUser.getId(), e);
            toastService.error(model, "Se ha producido un error al crear el cuestionario.");
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.misCuestionariosNuevo());
            return "admin/cuestionarios/quiz-form";
        }

        return "redirect:/mis-cuestionarios";
    }

    // -------------------------------------------------------------------------
    // Editar cuestionario personal
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/editar")
    public String showEditQuizForm(
            @PathVariable Long id,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Model model) {

        Cuestionario cuestionario = cuestionarioService.getPersonalQuizById(id, authUser.getId())
                .orElse(null);

        if (cuestionario == null) {
            toastService.error(model, "No se ha encontrado el cuestionario solicitado.");
            return "redirect:/mis-cuestionarios";
        }

        if (!cuestionarioService.isLastVersion(cuestionario)) {
            toastService.error(model, "Esta versión del cuestionario ya no es la más reciente.");
            return "redirect:/mis-cuestionarios";
        }

        QuizForm quizForm = quizFormService.createQuizForm(cuestionario);
        model.addAttribute("quizForm", quizForm);
        loadFormOptions(model, quizForm);
        breadcrumbContext.set(breadcrumbService.misCuestionariosEditar(cuestionario.getTitulo()));

        return "admin/cuestionarios/quiz-form";
    }

    @PostMapping("/{id}/editar")
    public String editPersonalQuiz(
            @PathVariable Long id,
            @Valid @ModelAttribute("quizForm") QuizForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Model model,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.misCuestionariosEditar(form.getTitulo()));
            return "admin/cuestionarios/quiz-form";
        }

        // Verificar que el cuestionario pertenece al usuario autenticado
        if (cuestionarioService.getPersonalQuizById(id, authUser.getId()).isEmpty()) {
            toastService.error(ra, "No tienes permiso para editar este cuestionario.");
            return "redirect:/mis-cuestionarios";
        }

        try {
            cuestionarioService.updateQuiz(id, form);
            toastService.success(ra, "El cuestionario se ha actualizado correctamente.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            toastService.error(model, e.getMessage());
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.misCuestionariosEditar(form.getTitulo()));
            return "admin/cuestionarios/quiz-form";
        } catch (DataIntegrityViolationException e) {
            toastService.error(model, "No se pudo actualizar el cuestionario por un conflicto de identificador interno. Inténtalo de nuevo.");
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.misCuestionariosEditar(form.getTitulo()));
            return "admin/cuestionarios/quiz-form";
        } catch (Exception e) {
            log.error("Error al actualizar cuestionario {} del usuario {}", id, authUser.getId(), e);
            toastService.error(model, "Se ha producido un error al actualizar el cuestionario.");
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.misCuestionariosEditar(form.getTitulo()));
            return "admin/cuestionarios/quiz-form";
        }

        return "redirect:/mis-cuestionarios";
    }

    // -------------------------------------------------------------------------
    // Eliminar cuestionario personal
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/eliminar")
    public String deletePersonalQuiz(
            @PathVariable Long id,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Pageable pageable,
            Model model,
            RedirectAttributes ra,
            HttpServletResponse response,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {

        // Verificar que el cuestionario pertenece al usuario autenticado
        if (cuestionarioService.getPersonalQuizById(id, authUser.getId()).isEmpty()) {
            String errorMsg = "No tienes permiso para eliminar este cuestionario.";
            if (hxRequest != null) {
                toastService.error(response, errorMsg);
            } else {
                toastService.error(ra, errorMsg);
            }
            return hxRequest != null ? null : "redirect:/mis-cuestionarios";
        }

        try {
            cuestionarioService.deleteQuizVersion(id);
            String successMsg = "El cuestionario se ha eliminado correctamente.";
            if (hxRequest != null) {
                toastService.success(response, successMsg);
            } else {
                toastService.success(ra, successMsg);
            }
        } catch (QuizlyException | EntityNotFoundException e) {
            if (hxRequest != null) {
                toastService.error(response, e.getMessage());
            } else {
                toastService.error(ra, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error al eliminar cuestionario {} del usuario {}", id, authUser.getId(), e);
            String errorMsg = "Se ha producido un error al eliminar el cuestionario.";
            if (hxRequest != null) {
                toastService.error(response, errorMsg);
            } else {
                toastService.error(ra, errorMsg);
            }
        }

        if (hxRequest != null) {
            loadCuestionariosData(authUser.getId(), search, dificultadId, pageable, model);
            return "cuestionarios/mis-cuestionarios :: lista-cuestionarios_fragment";
        }

        return "redirect:/mis-cuestionarios";
    }

    // -------------------------------------------------------------------------
    // Compartir cuestionario personal
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/compartir")
    public String compartirPersonalQuiz(
            @PathVariable Long id,
            @RequestParam("destinatario") String destinatario,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            HttpServletResponse response,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {

        try {
            cuestionarioCompartidoService.compartirCuestionario(id, destinatario, authUser.getId());
            toastService.success(response, "Solicitud enviada correctamente a " + destinatario);
        } catch (QuizlyException e) {
            log.warn("Error al compartir cuestionario {}: {}", id, e.getMessage());
            toastService.error(response, e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al compartir cuestionario {}", id, e);
            toastService.error(response, "Ocurrió un error inesperado al compartir el cuestionario.");
        }

        response.setStatus(HttpServletResponse.SC_OK);
        return "cuestionarios/mis-cuestionarios :: #empty-response";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void loadCuestionariosData(Long usuarioId, String search, Long dificultadId, Pageable pageable, Model model) {
        Page<Cuestionario> cuestionariosPage =
                cuestionarioService.getCuestionariosByUsuario(usuarioId, search, dificultadId, pageable);

        model.addAttribute("cuestionariosPage", cuestionariosPage);
        model.addAttribute("favoriteSerieIds", favoritoService.getFavoriteSerieIds(usuarioId, cuestionariosPage.getContent()));
        model.addAttribute("dificultades", dificultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("search", search);
        model.addAttribute("dificultadId", dificultadId);
    }

    private void loadFormOptions(Model model, QuizForm quizForm) {
        model.addAttribute("canImportQuizJson", false);
        QuizFormOptions options = quizFormService.getOptions(quizForm.getAsignaturaId());
        model.addAttribute("asignaturas", options.asignaturas());
        model.addAttribute("temas", options.tema());
        model.addAttribute("dificultades", options.dificultad());
        model.addAttribute("tiposPregunta", options.tiposPregunta());

        // URLs del formulario: action apunta a /mis-cuestionarios, no a /admin
        String formAction = quizForm.getId() == null
                ? "/mis-cuestionarios/nuevo"
                : "/mis-cuestionarios/" + quizForm.getId() + "/editar";
        model.addAttribute("formAction", formAction);
        model.addAttribute("cancelUrl", "/mis-cuestionarios");
        model.addAttribute("opcionesTemaUrl", "/mis-cuestionarios/opcionesTema");
    }

    private Dificultad resolveDificultad(String dificultadParam) {
        if (dificultadParam == null || dificultadParam.isBlank()) {
            return dificultadRepository.findByCodigo("MEDIUM").orElse(null);
        }
        try {
            Long id = Long.valueOf(dificultadParam);
            return dificultadRepository.findById(id).orElseGet(() ->
                    dificultadRepository.findByCodigo("MEDIUM").orElse(null));
        } catch (NumberFormatException e) {
            return dificultadRepository.findByCodigo(dificultadParam.toUpperCase()).orElseGet(() ->
                    dificultadRepository.findByCodigo("MEDIUM").orElse(null));
        }
    }
}
