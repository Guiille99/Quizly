package com.quizly.app.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizly.app.dto.form.TemaFilterForm;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.service.AdminTemaService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/admin/temas")
@RequiredArgsConstructor
public class AdminTemaController {
    private final AdminTemaService adminTemaService;
    private final AsignaturaRepository asignaturaRepository;

    @GetMapping
    public String showTemasList(@ModelAttribute TemaFilterForm filters, Pageable pageable, Model model){
        loadTemasTableAttributes(filters, pageable, model);
        return "admin/temas/temas-list";
    }

    @GetMapping("/tableData")
    public String getTemaTableData(@ModelAttribute TemaFilterForm filters, Pageable pageable, Model model,
        HttpServletRequest request) {

        // Obtenemos el campo que ha lanzado el evento
        String triggeredField = request.getHeader("HX-Trigger-Name");
        
        model.addAttribute("temasPage", adminTemaService.getTemas(filters, pageable));
        model.addAttribute("filters", filters);
        
        // Si limpiamos los filtros devolvemos el fragmente del contenedor (filtros + tabla)
        // para que se actualicen ambas cosas
        if ("resetFilters".equals(triggeredField)) {
            model.addAttribute("asignaturas", asignaturaRepository.findAllByOrderByNombreAsc());
            return "admin/temas/temas-list :: table-temas-container_fragment";
        }

        return "admin/temas/temas-list :: table-temas_fragment";
    }
    

    private void loadTemasTableAttributes(TemaFilterForm filters, Pageable pageable, Model model){
        model.addAttribute("temasPage", adminTemaService.getTemas(filters, pageable));
        model.addAttribute("asignaturas", asignaturaRepository.findAllByOrderByNombreAsc());
        model.addAttribute("filters", filters);
    }
}
