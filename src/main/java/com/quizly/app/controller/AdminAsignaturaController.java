package com.quizly.app.controller;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizly.app.dto.form.AsignaturaFilterForm;
import com.quizly.app.dto.form.AsignaturaForm;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.service.AdminAsignaturaService;
import com.quizly.app.service.AsignaturaService;
import com.quizly.app.web.notification.ToastService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller 
@RequestMapping("/admin/asignaturas")
@RequiredArgsConstructor 
public class AdminAsignaturaController {
    private final ToastService toastService;
    private final AdminAsignaturaService adminAsignaturaService;
    private final AsignaturaService asignaturaService;
    
    private final AsignaturaRepository asignaturaRepository;

    @GetMapping
    public String showAsignaturasList(@ModelAttribute AsignaturaFilterForm filters, Pageable pageable, Model model) {
        loadAsignaturasTableAttributes(filters, pageable, model);
        return "/admin/asignaturas/asignaturas-list";
    }

    @GetMapping("/tableData")
    public String getAsignaturasTableData(@ModelAttribute AsignaturaFilterForm filters, Pageable pageable, Model model,
        HttpServletRequest request) {
        
        String triggeredField = request.getHeader("HX-Trigger-Name");
        
        model.addAttribute("asignaturasPage", adminAsignaturaService.getAsignaturas(filters, pageable));
        model.addAttribute("filters", filters);

        if ("resetFilters".equals(triggeredField)) {
            return "admin/asignaturas/asignaturas-list :: table-asignaturas-container_fragment";
        }

        return "admin/asignaturas/asignaturas-list :: table-asignaturas_fragment";
    }
    

    private void loadAsignaturasTableAttributes(AsignaturaFilterForm filters, Pageable pageable, Model model) {
        model.addAttribute("asignaturasPage", adminAsignaturaService.getAsignaturas(filters, pageable));
        model.addAttribute("filters", filters);
    }
    
    @GetMapping("/nuevo")
    public String showCreateAsignaturaForm(Model model) {
        AsignaturaForm form = adminAsignaturaService.createAsignaturaForm();
        model.addAttribute("asignaturaForm", form);
        return "admin/asignaturas/asignatura-form";
    }

    @PostMapping("/nuevo")
    public String createNewAsignatura(@Valid @ModelAttribute("asignaturaForm") AsignaturaForm form,
        BindingResult bindingResult, Model model, RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            return "admin/asignaturas/asignatura-form";
        }

        try {
            asignaturaService.createAsignatura(form);
            toastService.success(ra, "La asignatura se ha creado correctamente.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            toastService.error(model, e.getMessage());
            return "admin/asignaturas/asignatura-form";
        } catch (Exception e){
            toastService.error(model, "Se ha producido un error al crear la asignatura.");
            return "admin/asignaturas/asignatura-form";
        }

        return "redirect:/admin/asignaturas";
    }
    
    @GetMapping("{id}/editar")
    public String showEditAsignaturaForm(@PathVariable Long id, Model model) {
        Optional<Asignatura> asignatura = asignaturaRepository.findById(id);

        if (!asignatura.isPresent()) {
            toastService.error(model, "No se ha encontrado la asignatura seleccionada.");
            return "admin/asignaturas/asignaturas-list";
        }

        AsignaturaForm form = adminAsignaturaService.createAsignaturaForm(asignatura.get());
        model.addAttribute("asignaturaForm", form);
        return "admin/asignaturas/asignatura-form";
    }

    @PostMapping("{id}/editar")
    public String editAsignatura(@PathVariable Long id, @Valid @ModelAttribute("asignaturaForm") AsignaturaForm form,
        BindingResult bindingResult, Model model, RedirectAttributes ra) {
        
        if (bindingResult.hasErrors()) {
            return "admin/asignaturas/asignatura-form";
        }

        try {
            asignaturaService.updateAsignatura(id, form);
            toastService.success(ra, "La asignatura se ha actualizado correctamente.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            toastService.error(model, e.getMessage());
            return "admin/asignaturas/asignatura-form";
        } catch (Exception e){
            toastService.error(model, "Se ha producido un error al actualizar la asignatura.");
            return "admin/asignaturas/asignatura-form";
        }

        return "redirect:/admin/asignaturas";        
    }

    @PostMapping("{id}/eliminar")
    public String deleteAsignatura(@PathVariable Long id, @ModelAttribute AsignaturaFilterForm filters, 
        Pageable pageable, Model model, RedirectAttributes ra, HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
            
        try {
            asignaturaService.deleteAsignatura(id);
            String successMsg = "La asignatura se ha eliminado correctamente.";

            if (hxRequest != null) {
                toastService.success(response, successMsg);
            } else{
                toastService.success(ra, successMsg);
            }
        } catch (QuizlyException | EntityNotFoundException e) {
            if (hxRequest != null) {
                toastService.error(response, e.getMessage());
            } else {
                toastService.error(ra, e.getMessage());
            }
        } catch (Exception e) {
            String errorMsg = "Se ha producido un error al eliminar la asignatura.";

            if (hxRequest != null) {
                toastService.error(response, errorMsg);    
            } else {
                toastService.error(ra, errorMsg);
            }
        }
        
        return hxRequest != null 
            ? refreshAsignaturaTable(filters, pageable, model) 
            : "redirect:/admin/asignaturas";
    }

    private String refreshAsignaturaTable(AsignaturaFilterForm filters, Pageable pageable, Model model){
        loadAsignaturasTableAttributes(filters, pageable, model);
        return "admin/asignaturas/asignaturas-list :: table-asignaturas-container_fragment"; 
    }
}
