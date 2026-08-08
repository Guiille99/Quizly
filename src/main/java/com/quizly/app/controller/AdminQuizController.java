package com.quizly.app.controller;

import com.quizly.app.dto.form.QuizFilterForm;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.service.AdminQuizService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/admin/cuestionarios")
@RequiredArgsConstructor
public class AdminQuizController {

    private final AsignaturaRepository asignaturaRepository;
    private final AdminQuizService adminQuizService;
    private final TemaRepository temaRepository;

    @GetMapping
    public String showCuestionariosList(@ModelAttribute QuizFilterForm filters, Pageable pageable, Model model) {
        model.addAttribute("cuestionariosPage", adminQuizService.getCuestionarios(filters, pageable));
        model.addAttribute("filters", filters);
        model.addAttribute("asignaturas", asignaturaRepository.findAllByOrderByNombreAsc());

        return "admin/cuestionarios/cuestionarios-list";
    }

    @GetMapping("/tableData")
    public String getCuestionariosTableData(@ModelAttribute QuizFilterForm filters, Pageable pageable, Model model,
        HttpServletRequest request, HttpServletResponse response) {

        // Obtenemos el campo que ha lanzado el evento
        String triggeredField = request.getHeader("HX-Trigger-Name");

        // Si la asignatura cambia o pasa a filtrarse por todas se elimina el filtro por tema
        if (!"resetFilters".equals(triggeredField) 
            && ("asignaturaId".equals(triggeredField) || filters.asignaturaId() == null)) {
            filters = filters.toBuilder().temaId(null).build();
        }

        model.addAttribute("cuestionariosPage", adminQuizService.getCuestionarios(filters, pageable));
        model.addAttribute("filters", filters);

        
        // Si limpiamos los filtros devolvemos el fragmente del contenedor (filtros + tabla)
        // para que se actualicen ambas cosas
        if ("resetFilters".equals(triggeredField)) {
            model.addAttribute("asignaturas", asignaturaRepository.findAllByOrderByNombreAsc());
            return "admin/cuestionarios/cuestionarios-list :: table-cuestionarios-container_fragment";               
        }

        // Emitimos el evento de actualización de temas si cambió el filtro de asignatura
        if ("asignaturaId".equals(triggeredField)) {
            response.setHeader("HX-Trigger", "actualizarOpcionesTema");
        }
    

        return "admin/cuestionarios/cuestionarios-list :: table-cuestionarios_fragment";
    }

    @GetMapping("/opcionesTema")
    public String getOpcionesTemasFiltro(@ModelAttribute QuizFilterForm filters, Model model) {
        // El filtro de los temas cargará los temas de la asignatura filtrada
        // Si el filtro por asignatura no está aplicado se devolverá una lista vacía
        model.addAttribute("temas", filters.asignaturaId() != null 
            ? temaRepository.findByAsignaturaId(filters.asignaturaId()) 
            : List.of());

        model.addAttribute("filters", filters);

        return "admin/cuestionarios/cuestionarios-list :: opciones-temas-filtro_fragment";
    }
    
    
}
