package com.quizly.app.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.dto.ai.MazoGenerado;
import com.quizly.app.dto.form.MazoForm;
import com.quizly.app.exception.AiGenerationException;
import com.quizly.app.exception.AiHighDemandException;
import com.quizly.app.exception.PdfProcessingException;
import com.quizly.app.security.RateLimiter;
import com.quizly.app.service.AiFlashcardGeneratorService;
import com.quizly.app.service.PdfExtractorService;
import com.quizly.app.model.entity.Mazo;
import com.quizly.app.model.enums.Calificacion;
import com.quizly.app.model.view.tarjeta.ResultadoImportacion;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.BreadcrumbService;
import com.quizly.app.service.MazoService;
import com.quizly.app.util.Constantes;
import com.quizly.app.web.model.BreadcrumbContext;
import com.quizly.app.web.notification.ToastService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/mazos")
public class TarjetasController {

    private static final String VIEW_LISTA = "tarjetas/mazos";
    private static final String VIEW_FORM = "tarjetas/mazo-form";
    private static final String VIEW_ESTUDIAR = "tarjetas/estudiar";
    private static final String FRAGMENT_TARJETA = "tarjetas/estudiar :: tarjeta_fragment";

    // Mismos límites que la generación de cuestionarios con IA
    private static final int IA_MAX_POR_HORA = 10;
    private static final int IA_MAX_POR_DIA = 40;
    private static final int IA_MAX_SIMULTANEAS = 2;
    private final Semaphore iaSlots = new Semaphore(IA_MAX_SIMULTANEAS);

    private final MazoService mazoService;
    private final PdfExtractorService pdfExtractorService;
    private final AiFlashcardGeneratorService aiFlashcardGeneratorService;
    private final RateLimiter rateLimiter;
    private final BreadcrumbContext breadcrumbContext;
    private final BreadcrumbService breadcrumbService;
    private final ToastService toastService;

    @Value("${ai.max-file-size-mb:20}")
    private int maxFileSizeMb;

    @ModelAttribute("maxFileSizeMb")
    public int maxFileSizeMb() {
        return maxFileSizeMb;
    }

    // =========================================================================
    // Mazos
    // =========================================================================

    @GetMapping
    public String listar(@AuthenticationPrincipal QuizlyUserDetails user, Model model, HttpServletRequest request) {
        model.addAttribute("mazos", mazoService.listar(user.getId()));
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.mazos());
        return VIEW_LISTA;
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model, HttpServletRequest request) {
        model.addAttribute("mazoForm", new MazoForm());
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.mazoNuevo());
        return VIEW_FORM;
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("mazoForm") MazoForm form, BindingResult result,
            @AuthenticationPrincipal QuizlyUserDetails user, Model model, HttpServletRequest request,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
            breadcrumbContext.set(breadcrumbService.mazoNuevo());
            return VIEW_FORM;
        }
        Mazo mazo = mazoService.crear(user.getId(), form.getTitulo(), form.getDescripcion());
        toastService.success(ra, "Mazo creado. Ya puedes añadir tarjetas.");
        return "redirect:/mazos/" + mazo.getId() + "/editar";
    }

    @GetMapping("/{mazoId}/editar")
    public String editar(@PathVariable Long mazoId, @RequestParam(required = false) String nueva,
            @AuthenticationPrincipal QuizlyUserDetails user, Model model, HttpServletRequest request) {
        Mazo mazo = mazoService.obtener(mazoId, user.getId());
        model.addAttribute("enfocarNueva", nueva != null);
        MazoForm form = new MazoForm();
        form.setId(mazo.getId());
        form.setTitulo(mazo.getTitulo());
        form.setDescripcion(mazo.getDescripcion());
        cargarEdicion(model, mazo, form, user.getId(), request);
        return VIEW_FORM;
    }

    @PostMapping("/{mazoId}")
    public String actualizar(@PathVariable Long mazoId, @Valid @ModelAttribute("mazoForm") MazoForm form,
            BindingResult result, @AuthenticationPrincipal QuizlyUserDetails user, Model model,
            HttpServletRequest request, RedirectAttributes ra) {
        Mazo mazo = mazoService.obtener(mazoId, user.getId());
        if (result.hasErrors()) {
            form.setId(mazoId);
            cargarEdicion(model, mazo, form, user.getId(), request);
            return VIEW_FORM;
        }
        try {
            mazoService.guardarCambios(mazoId, user.getId(), form.getTitulo(), form.getDescripcion(),
                    form.getTarjetaId(), form.getAnverso(), form.getReverso(), form.getNuevoAnverso(),
                    form.getNuevoReverso());
            toastService.success(ra, "Cambios guardados.");
        } catch (IllegalArgumentException e) {
            toastService.error(ra, e.getMessage());
        }
        return "redirect:/mazos/" + mazoId + "/editar" + (form.isSeguir() ? "?nueva" : "");
    }

    @PostMapping("/{mazoId}/eliminar")
    public String eliminar(@PathVariable Long mazoId, @AuthenticationPrincipal QuizlyUserDetails user,
            RedirectAttributes ra) {
        if (mazoService.eliminar(mazoId, user.getId())) {
            toastService.success(ra, "Mazo eliminado.");
        } else {
            toastService.warning(ra, "El mazo de repaso de fallos no se puede eliminar.");
        }
        return "redirect:/mazos";
    }

    // Solo ROLE_ADMIN (SecurityConfig): la cuota gratuita de Gemini no permite servir a otros usuarios.
    @PostMapping("/generar-ia")
    public String generarIa(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "numTarjetas", defaultValue = "15") Integer numTarjetas,
            @AuthenticationPrincipal QuizlyUserDetails user, RedirectAttributes ra) {

        // Misma clave que la generación de cuestionarios: la cuota del proveedor es una sola
        String userKey = "ia:" + user.getId();
        if (!rateLimiter.tryAcquire(userKey + ":h", IA_MAX_POR_HORA, Duration.ofHours(1))
                || !rateLimiter.tryAcquire(userKey + ":d", IA_MAX_POR_DIA, Duration.ofDays(1))) {
            toastService.error(ra, "Has alcanzado el límite de generaciones con IA. Inténtalo de nuevo más tarde.");
            return "redirect:/mazos";
        }
        // shortcut: semáforo propio, hasta 2 generaciones simultáneas aquí además de las 2 de cuestionarios; compartirlo si molesta
        if (!iaSlots.tryAcquire()) {
            toastService.error(ra, "Hay demasiadas generaciones en curso. Inténtalo de nuevo en unos minutos.");
            return "redirect:/mazos";
        }

        try {
            String texto = pdfExtractorService.extractTextFromPdf(file);
            MazoGenerado generado = aiFlashcardGeneratorService
                    .generateFlashcardsFromText(texto, file.getOriginalFilename(), numTarjetas);
            Mazo mazo = mazoService.crearDesdeIa(user.getId(), generado);
            toastService.success(ra, "Mazo generado con " + generado.tarjetas().size() + " tarjetas. Revísalo antes de estudiar.");
            return "redirect:/mazos/" + mazo.getId() + "/editar";
        } catch (PdfProcessingException e) {
            log.warn("Fallo al procesar PDF subido por el usuario: {}", e.getMessage());
            toastService.error(ra, e.getMessage());
        } catch (AiHighDemandException e) {
            log.warn("El proveedor IA está saturado", e);
            toastService.error(ra, "El servicio de IA está experimentando una alta demanda en este momento. Por favor, inténtelo de nuevo más tarde.");
        } catch (AiGenerationException e) {
            log.error("Error al generar tarjetas con IA: {}", e.getMessage(), e);
            toastService.error(ra, "Se produjo un error interno al generar las tarjetas con IA, inténtelo de nuevo más tarde");
        } catch (Exception e) {
            log.error("Error inesperado en la generación de tarjetas con IA", e);
            toastService.error(ra, "Se ha producido un error inesperado al procesar el archivo.");
        } finally {
            iaSlots.release();
        }
        return "redirect:/mazos";
    }

    @PostMapping("/importar/{slug}")
    public String importar(@PathVariable String slug, @AuthenticationPrincipal QuizlyUserDetails user,
            RedirectAttributes ra) {
        ResultadoImportacion resultado = mazoService.importarDesdeCuestionario(slug, user.getId());
        if (resultado.mazoId() == null) {
            toastService.warning(ra, "Este cuestionario no tiene preguntas con respuesta correcta: no se ha creado ningún mazo.");
            return "redirect:/cuestionarios/" + slug;
        }

        String detalle = detalleOmitidas(resultado);
        if (resultado.importadas() == 0) {
            toastService.info(ra, "No hay tarjetas nuevas" + detalle + ".");
        } else {
            toastService.success(ra, "Tarjetas añadidas: " + resultado.importadas() + detalle + ".");
        }
        return "redirect:/mazos/" + resultado.mazoId() + "/editar";
    }

    private String detalleOmitidas(ResultadoImportacion resultado) {
        List<String> motivos = new ArrayList<>();
        if (resultado.yaExistentes() > 0) {
            motivos.add(resultado.yaExistentes() + " ya existían");
        }
        if (resultado.sinRespuesta() > 0) {
            motivos.add(resultado.sinRespuesta() + " sin respuesta correcta");
        }
        return motivos.isEmpty() ? "" : " (" + String.join(", ", motivos) + ")";
    }

    // =========================================================================
    // Tarjetas
    // =========================================================================

    @PostMapping("/{mazoId}/tarjetas/{tarjetaId}/eliminar")
    public String eliminarTarjeta(@PathVariable Long mazoId, @PathVariable Long tarjetaId,
            @AuthenticationPrincipal QuizlyUserDetails user, RedirectAttributes ra) {
        mazoService.eliminarTarjeta(mazoId, tarjetaId, user.getId());
        toastService.success(ra, "Tarjeta eliminada.");
        return "redirect:/mazos/" + mazoId + "/editar";
    }

    // =========================================================================
    // Estudio
    // =========================================================================

    @GetMapping("/{mazoId}/estudiar")
    public String estudiar(@PathVariable Long mazoId, @AuthenticationPrincipal QuizlyUserDetails user, Model model,
            HttpServletRequest request) {
        Mazo mazo = mazoService.obtener(mazoId, user.getId());
        model.addAttribute("mazo", mazo);
        model.addAttribute("estado", mazoService.siguiente(mazoId, user.getId()));
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.mazoEstudiar(mazo.getTitulo()));
        return VIEW_ESTUDIAR;
    }

    @GetMapping("/{mazoId}/siguiente")
    public String siguiente(@PathVariable Long mazoId, @AuthenticationPrincipal QuizlyUserDetails user,
            Model model) {
        model.addAttribute("mazo", mazoService.obtener(mazoId, user.getId()));
        model.addAttribute("estado", mazoService.siguiente(mazoId, user.getId()));
        return FRAGMENT_TARJETA;
    }

    @PostMapping("/{mazoId}/tarjetas/{tarjetaId}/calificar")
    public String calificar(@PathVariable Long mazoId, @PathVariable Long tarjetaId,
            @RequestParam Calificacion calificacion, @AuthenticationPrincipal QuizlyUserDetails user, Model model) {
        model.addAttribute("mazo", mazoService.obtener(mazoId, user.getId()));
        model.addAttribute("estado", mazoService.calificar(mazoId, tarjetaId, calificacion, user.getId()));
        return FRAGMENT_TARJETA;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void cargarEdicion(Model model, Mazo mazo, MazoForm form, Long usuarioId, HttpServletRequest request) {
        model.addAttribute("mazoForm", form);
        model.addAttribute("mazo", mazo);
        model.addAttribute("tarjetas", mazoService.tarjetas(mazo.getId(), usuarioId));
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.mazoEditar(mazo.getTitulo()));
    }
}
