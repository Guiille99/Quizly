package com.quizly.app.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.quizly.app.dto.AsignaturaProgresoDto;
import com.quizly.app.dto.TemaProgresoDto;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.AsignaturaService;
import com.quizly.app.service.BreadcrumbService;
import com.quizly.app.service.CuestionarioService;
import com.quizly.app.service.TemaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.util.Constantes;
import com.quizly.app.web.model.BreadcrumbContext;
import com.quizly.app.web.notification.ToastService;

import jakarta.servlet.http.HttpServletRequest;


@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/asignaturas")
public class AsignaturasController {

    private final AsignaturaService asignaturaService;
    private final TemaService temaService;
    private final CuestionarioService cuestionarioService;
    private final ToastService toastService;
    private final BreadcrumbContext breadcrumbContext;
    private final BreadcrumbService breadcrumbService;

    @GetMapping
    public String showAsignaturas(Model model, HttpServletRequest request,
        @AuthenticationPrincipal QuizlyUserDetails userDetails, Pageable pageable) {
        
        Page<AsignaturaProgresoDto> asignaturasProgreso = asignaturaService.getAsignaturasProgresoUsuarioPaginated(
            userDetails.getUsuario(), null, pageable);

        model.addAttribute("asignaturas", asignaturasProgreso);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.asignaturas());
        return "asignaturas/asignaturas";
    }
    
    @GetMapping("/elementos")
    public String getAsignaturasElementos(Model model, 
        @AuthenticationPrincipal QuizlyUserDetails userDetails, Pageable pageable,
        @RequestParam(required = false) String nameFilter) {

        Page<AsignaturaProgresoDto> asignaturasProgreso = asignaturaService.getAsignaturasProgresoUsuarioPaginated(
            userDetails.getUsuario(), nameFilter, pageable);

        model.addAttribute("asignaturas", asignaturasProgreso);

        return "asignaturas/asignaturas :: lista-asignaturas_fragment";
    }

    @GetMapping("/{slug}")
    public String showAsignaturaBySlug(@PathVariable String slug, @AuthenticationPrincipal QuizlyUserDetails authUser, 
           Pageable pageable, Model model, RedirectAttributes ra, HttpServletRequest request) {
        Asignatura asignatura = asignaturaService.getAsignatruaBySlug(slug);

        // Si no existe ninguna asignatura con el slug recibido
        if (asignatura == null) {
            log.warn("El usuario '{}' intentó acceder a una asignatura inexistente con slug '{}'", authUser.getUsername(), slug);
            toastService.error(ra, "No se ha encontrado la asignatura solicitada.");
            return "redirect:/asignaturas";
        }

        Page<TemaProgresoDto> temasAsignatura = temaService.getTemasProgresoPaginatedByUsuarioYAsignatura(authUser.getUsuario(), asignatura, 
            null, pageable);

        model.addAttribute("asignatura", asignatura);
        model.addAttribute("temasAsignatura", temasAsignatura);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.asignatura(asignatura));

        return "asignaturas/temas_asignatura";
    }
    
    @GetMapping("{slug}/tableData")
    public String getTemasAsignaturasTableData(@PathVariable String slug, @AuthenticationPrincipal QuizlyUserDetails authUser, 
        @RequestParam(required = false) String nameFilter, 
        Pageable pageable, Model model) {

        Asignatura asignatura = asignaturaService.getAsignatruaBySlug(slug);
    
        Page<TemaProgresoDto> temasAsignatura = temaService.getTemasProgresoPaginatedByUsuarioYAsignatura(authUser.getUsuario(), asignatura, 
            nameFilter, pageable);

        model.addAttribute("asignatura", asignatura);
        model.addAttribute("temasAsignatura", temasAsignatura);

        return "asignaturas/temas_asignatura :: lista-temas_fragment";
    }

    @GetMapping("{slugAsig}/{slugTema}")
    public String showCuestionariosTemaAsignatura(@PathVariable String slugAsig, @PathVariable String slugTema, 
        @AuthenticationPrincipal QuizlyUserDetails authUser, Pageable pageable, 
        Model model, RedirectAttributes ra, HttpServletRequest request) {
        
        Asignatura asignatura = asignaturaService.getAsignatruaBySlug(slugAsig);
        Tema tema = temaService.getTemaBySlug(slugTema);

        if (asignatura == null || tema == null) {
            log.warn("El usuario '{}' intentó acceder a un tema inexistente. Slug de la asignatura: '{}', slug del tema: '{}'",
                authUser.getUsername(), slugAsig, slugTema);
            toastService.error(ra, "No se ha encontrado la asignatura o tema solicitado.");
            return "redirect:/asignaturas";
        }

        Page<Cuestionario> cuestionariosTemaPage = cuestionarioService.getCuestionariosPaginatedByTema(tema, null, pageable);

        model.addAttribute("asignatura", asignatura);
        model.addAttribute("tema", tema);
        model.addAttribute("cuestionariosTemaPage", cuestionariosTemaPage);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.temaAsignatura(tema));

        return "asignaturas/cuestionarios_tema_asignatura";
    }

    @GetMapping("{slugAsig}/{slugTema}/tableData")
    public String getCuestionariosTemaAsignaturaTableData(@PathVariable String slugAsig, @PathVariable String slugTema,
        @AuthenticationPrincipal QuizlyUserDetails authUser, @RequestParam(required = false) String titleFilter, 
        Pageable pageable, Model model) {
        
        Asignatura asignatura = asignaturaService.getAsignatruaBySlug(slugAsig);
        Tema tema = temaService.getTemaBySlug(slugTema);

        Page<Cuestionario> cuestionariosTemaPage = cuestionarioService.getCuestionariosPaginatedByTema(tema, titleFilter, pageable);

        model.addAttribute("asignatura", asignatura);
        model.addAttribute("tema", tema);
        model.addAttribute("cuestionariosTemaPage", cuestionariosTemaPage);

        return "asignaturas/cuestionarios_tema_asignatura :: lista-cuestionarios_fragment";
    }
}
