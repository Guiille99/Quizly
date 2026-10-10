package com.quizly.app.controller;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizly.app.dto.form.TemaFilterForm;
import com.quizly.app.dto.form.TemaForm;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.service.AdminTemaService;
import com.quizly.app.service.TemaService;
import com.quizly.app.web.notification.ToastService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;




@Controller
@RequestMapping("/admin/temas")
@RequiredArgsConstructor
public class AdminTemaController {
    private final ToastService toastService;
    private final AdminTemaService adminTemaService;
    private final TemaService temaService;

    private final AsignaturaRepository asignaturaRepository;
    private final TemaRepository temaRepository;
    private final com.quizly.app.web.model.BreadcrumbContext breadcrumbContext;
    private final com.quizly.app.service.BreadcrumbService breadcrumbService;

    @GetMapping
    public String showTemasList(@ModelAttribute TemaFilterForm filters, Pageable pageable, Model model){
        loadTemasTableAttributes(filters, pageable, model);
        breadcrumbContext.set(breadcrumbService.adminTemas());
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
        TemaForm form = adminTemaService.createTemaForm();
        model.addAttribute("temaForm", form);
        loadFormOptions(model, form);
        breadcrumbContext.set(breadcrumbService.adminTemaNuevo());
        return "admin/temas/tema-form";
    }

    @PostMapping("/nuevo")
    public String createNewTema(@Valid @ModelAttribute("temaForm") TemaForm form,
        BindingResult bindingResult, Model model, RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.adminTemaNuevo());
            return "admin/temas/tema-form";
        }

        try {
            temaService.createTema(form);
            toastService.success(ra, "El tema se ha creado correctamente.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            toastService.error(model, e.getMessage());
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.adminTemaNuevo());
            return "admin/temas/tema-form";
        } catch (Exception e){
            toastService.error(model, "Se ha producido un error al crear el tema.");
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.adminTemaNuevo());
            return "admin/temas/tema-form";
        }

        return "redirect:/admin/temas";
    }
    
    

    @GetMapping("{id}/editar")
    public String showEditTemaForm(@PathVariable Long id, Model model) {
        Optional<Tema> tema = temaRepository.findById(id);

        if (!tema.isPresent()) {
            toastService.error(model, "No se ha encontrado el tema seleccionado.");
            return "admin/temas/temas-list";
        }

        TemaForm form = adminTemaService.createTemaForm(tema.get());

        model.addAttribute("temaForm", form);
        loadFormOptions(model, form);
        breadcrumbContext.set(breadcrumbService.adminTemaEditar(tema.get().getNombre()));
        return "admin/temas/tema-form";
    }

    @PostMapping("{id}/editar")
    public String editTema(@PathVariable Long id, @Valid @ModelAttribute("temaForm") TemaForm form,
        BindingResult bindingResult, Model model, RedirectAttributes ra) {
        
        if (bindingResult.hasErrors()) {
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.adminTemaEditar(form.getNombre()));
            return "admin/temas/tema-form";
        }

        try {
            temaService.updateTema(id, form);
            toastService.success(ra, "El tema se ha actualizado correctamente.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            toastService.error(model, e.getMessage());
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.adminTemaEditar(form.getNombre()));
            return "admin/temas/tema-form";
        } catch (Exception e){
            toastService.error(model, "Se ha producido un error al actualizar el tema.");
            loadFormOptions(model, form);
            breadcrumbContext.set(breadcrumbService.adminTemaEditar(form.getNombre()));
            return "admin/temas/tema-form";
        }

        return "redirect:/admin/temas";        
    }
    

    @PostMapping("{id}/eliminar")
    public String deleteTema(@PathVariable Long id, @ModelAttribute TemaFilterForm filters, 
        Pageable pageable, Model model, RedirectAttributes ra, HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
                
            
        try {
            temaService.deleteTema(id);
            String successMsg = "El tema se ha eliminado correctamente.";

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
            String errorMsg = "Se ha producido un error al eliminar el tema.";

            if (hxRequest != null) {
                toastService.error(response, errorMsg);    
            } else {
                toastService.error(ra, errorMsg);
            }
        }
        
        return hxRequest != null 
            ? refreshTemaTable(filters, pageable, model) 
            : "redirect:/admin/cuestionarios";
    }
    
    private void loadFormOptions(Model model, TemaForm form){
        model.addAttribute("asignaturas", asignaturaRepository.findAllByOrderByNombreAsc());
    }

    private String refreshTemaTable(TemaFilterForm filters, Pageable pageable, Model model){
        loadTemasTableAttributes(filters, pageable, model);
        return "admin/temas/temas-list :: table-temas-container_fragment"; 
    }
    
}
