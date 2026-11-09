package com.quizly.app.controller;

import java.time.Duration;
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

import com.quizly.app.dto.ai.MapaGenerado;
import com.quizly.app.dto.form.MapaMentalForm;
import com.quizly.app.exception.AiGenerationException;
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
@RequestMapping("/mapas")
public class MapasMentalesController {

    private static final String VIEW_LISTA = "mapas/mapas";
    private static final String VIEW_NUEVO = "mapas/mapa-nuevo";
    private static final String VIEW_EDITOR = "mapas/mapa-editor";

    // Mismos límites que la generación de cuestionarios y de tarjetas con IA
    private static final int IA_MAX_POR_HORA = 10;
    private static final int IA_MAX_POR_DIA = 40;
    private static final int IA_MAX_SIMULTANEAS = 2;
    private final Semaphore iaSlots = new Semaphore(IA_MAX_SIMULTANEAS);

    private final MapaMentalService mapaMentalService;
    private final PdfExtractorService pdfExtractorService;
    private final AiMindMapGeneratorService aiMindMapGeneratorService;
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

    @GetMapping
    public String listar(@RequestParam(required = false) Long asignatura,
            @AuthenticationPrincipal QuizlyUserDetails user, Model model, HttpServletRequest request) {
        model.addAttribute("listado", mapaMentalService.listado(user.getId(), asignatura));
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.mapas());
        return VIEW_LISTA;
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model, HttpServletRequest request) {
        model.addAttribute("mapaForm", new MapaMentalForm());
        model.addAttribute("temasPorAsignatura", mapaMentalService.temasPorAsignatura());
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.mapaNuevo());
        return VIEW_NUEVO;
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("mapaForm") MapaMentalForm form, BindingResult result,
            @AuthenticationPrincipal QuizlyUserDetails user, Model model, HttpServletRequest request,
            RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                MapaMental mapa = mapaMentalService.crear(user.getId(), form.getTitulo(), form.getDescripcion(),
                        form.getTemaId());
                toastService.success(ra, "Mapa creado. Ya puedes añadir ideas.");
                return "redirect:/mapas/" + mapa.getId();
            } catch (IllegalArgumentException e) {
                toastService.error(model, e.getMessage());
            }
        }
        model.addAttribute("temasPorAsignatura", mapaMentalService.temasPorAsignatura());
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.mapaNuevo());
        return VIEW_NUEVO;
    }

    @GetMapping("/{id}")
    public String editar(@PathVariable Long id, @AuthenticationPrincipal QuizlyUserDetails user, Model model,
            HttpServletRequest request) {
        MapaMental mapa = mapaMentalService.obtener(id, user.getId());
        MapaMentalForm form = new MapaMentalForm();
        form.setId(mapa.getId());
        form.setTitulo(mapa.getTitulo());
        form.setDescripcion(mapa.getDescripcion());
        form.setTemaId(mapa.getTema() == null ? null : mapa.getTema().getId());
        form.setContenido(mapa.getContenido());
        cargarEditor(model, mapa, form, request);
        return VIEW_EDITOR;
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("mapaForm") MapaMentalForm form,
            BindingResult result, @AuthenticationPrincipal QuizlyUserDetails user, Model model,
            HttpServletRequest request, RedirectAttributes ra) {
        MapaMental mapa = mapaMentalService.obtener(id, user.getId());
        form.setId(id);
        if (!result.hasErrors()) {
            try {
                mapaMentalService.guardar(id, user.getId(), form.getTitulo(), form.getDescripcion(),
                        form.getTemaId(), form.getContenido());
                toastService.success(ra, "Cambios guardados.");
                return "redirect:/mapas/" + id;
            } catch (IllegalArgumentException e) {
                toastService.error(model, e.getMessage());
            }
        }
        // Sin redirect: el editor se vuelve a pintar con lo que el usuario envió, para que no pierda su trabajo
        cargarEditor(model, mapa, form, request);
        return VIEW_EDITOR;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, @AuthenticationPrincipal QuizlyUserDetails user,
            RedirectAttributes ra) {
        mapaMentalService.eliminar(id, user.getId());
        toastService.success(ra, "Mapa eliminado.");
        return "redirect:/mapas";
    }

    // Solo ROLE_ADMIN (SecurityConfig): la cuota gratuita de Gemini no permite servir a otros usuarios.
    @PostMapping("/generar-ia")
    public String generarIa(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal QuizlyUserDetails user,
            RedirectAttributes ra) {

        // Misma clave que la generación de cuestionarios y de tarjetas: la cuota del proveedor es una sola
        String userKey = "ia:" + user.getId();
        if (!rateLimiter.tryAcquire(userKey + ":h", IA_MAX_POR_HORA, Duration.ofHours(1))
                || !rateLimiter.tryAcquire(userKey + ":d", IA_MAX_POR_DIA, Duration.ofDays(1))) {
            toastService.error(ra, "Has alcanzado el límite de generaciones con IA. Inténtalo de nuevo más tarde.");
            return "redirect:/mapas";
        }
        // shortcut: semáforo propio (como el de tarjetas), así que el total de generaciones simultáneas puede sumar
        // 2 por controlador; compartirlo si molesta
        if (!iaSlots.tryAcquire()) {
            toastService.error(ra, "Hay demasiadas generaciones en curso. Inténtalo de nuevo en unos minutos.");
            return "redirect:/mapas";
        }

        try {
            String texto = pdfExtractorService.extractTextFromPdf(file);
            MapaGenerado generado = aiMindMapGeneratorService.generateMindMapFromText(texto, file.getOriginalFilename());
            MapaMental mapa = mapaMentalService.crearDesdeIa(user.getId(), generado);
            toastService.success(ra, "Mapa generado con " + generado.nodos() + " nodos. Revísalo antes de estudiarlo.");
            return "redirect:/mapas/" + mapa.getId();
        } catch (PdfProcessingException e) {
            log.warn("Fallo al procesar PDF subido por el usuario: {}", e.getMessage());
            toastService.error(ra, e.getMessage());
        } catch (AiHighDemandException e) {
            log.warn("El proveedor IA está saturado", e);
            toastService.error(ra, "El servicio de IA está experimentando una alta demanda en este momento. Por favor, inténtelo de nuevo más tarde.");
        } catch (AiGenerationException e) {
            log.error("Error al generar el mapa mental con IA: {}", e.getMessage(), e);
            toastService.error(ra, "Se produjo un error interno al generar el mapa con IA, inténtelo de nuevo más tarde");
        } catch (Exception e) {
            log.error("Error inesperado en la generación de mapas mentales con IA", e);
            toastService.error(ra, "Se ha producido un error inesperado al procesar el archivo.");
        } finally {
            iaSlots.release();
        }
        return "redirect:/mapas";
    }

    @PostMapping("/{id}/crear-mazo")
    public String crearMazo(@PathVariable Long id, @AuthenticationPrincipal QuizlyUserDetails user,
            RedirectAttributes ra) {
        ResultadoMazoDesdeMapa resultado = mapaMentalService.crearMazo(id, user.getId());
        if (resultado.mazoId() == null) {
            toastService.warning(ra, "Este mapa no tiene ramas: añade ideas hijas para poder crear tarjetas.");
            return "redirect:/mapas/" + id;
        }
        if (resultado.creadas() < resultado.total()) {
            toastService.warning(ra, "Mazo creado con " + resultado.creadas() + " de " + resultado.total()
                    + " tarjetas: es el máximo por mazo. Revísalo antes de estudiar.");
        } else {
            toastService.success(ra, "Mazo creado con " + resultado.creadas()
                    + (resultado.creadas() == 1 ? " tarjeta." : " tarjetas."));
        }
        return "redirect:/mazos/" + resultado.mazoId() + "/editar";
    }

    private void cargarEditor(Model model, MapaMental mapa, MapaMentalForm form, HttpServletRequest request) {
        model.addAttribute("mapa", mapa);
        model.addAttribute("mapaForm", form);
        model.addAttribute("temasPorAsignatura", mapaMentalService.temasPorAsignatura());
        // El editor aplica los mismos límites que el servidor para avisar antes de enviar
        model.addAttribute("maxNodos", MapaMentalService.MAX_NODOS);
        model.addAttribute("maxProfundidad", MapaMentalService.MAX_PROFUNDIDAD);
        model.addAttribute("maxTopic", MapaMentalService.MAX_TOPIC);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.mapaEditar(mapa.getTitulo()));
    }
}
