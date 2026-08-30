package com.quizly.app.controller;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizly.app.dto.form.TemaFilterForm;
import com.quizly.app.dto.form.TemaForm;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.service.AdminTemaService;
import com.quizly.app.web.notification.ToastService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;



@Controller
@RequestMapping("/admin/temas")
@RequiredArgsConstructor
public class AdminTemaController {
    private final ToastService toastService;
    private final AdminTemaService adminTemaService;
    private final AsignaturaRepository asignaturaRepository;
    private final TemaRepository temaRepository;

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

    @GetMapping("/nuevo")
    public String showCreateTemaForm(Model model) {
        TemaForm form = adminTemaService.creatTemaForm();
        model.addAttribute("temaForm", form);
        loadFormOptions(model, form);
        return "admin/temas/tema-form";
    }
    

    @GetMapping("{id}/editar")
    public String showEditTemaForm(@PathVariable Long id, Model model) {
        Optional<Tema> tema = temaRepository.findById(id);

        if (!tema.isPresent()) {
            toastService.error(model, "No se ha encontrado el tema seleccionado.");
            return "admin/temas/tema-form";
        }

        return "admin/temas/tema-form";
    }

    private void loadFormOptions(Model model, TemaForm form){
        model.addAttribute("asignaturas", asignaturaRepository.findAllByOrderByNombreAsc());
    }
    
}
