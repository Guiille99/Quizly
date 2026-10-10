package com.quizly.app.controller;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.dto.form.UsuarioFilterForm;
import com.quizly.app.dto.form.UsuarioForm;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.Rol;
import com.quizly.app.repository.GeneroRepository;
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.service.AdminUsuarioService;
import com.quizly.app.service.UsuarioService;
import com.quizly.app.web.notification.ToastService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller 
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor 
public class AdminUsuarioController {
    private final ToastService toastService;
    private final AdminUsuarioService adminUsuarioService;
    private final UsuarioService usuarioService;

    private final UsuarioRepository usuarioRepository;
    private final GeneroRepository generoRepository;
    private final com.quizly.app.web.model.BreadcrumbContext breadcrumbContext;
    private final com.quizly.app.service.BreadcrumbService breadcrumbService;

    @GetMapping
    public String showUsuariosList(@ModelAttribute UsuarioFilterForm filters, Pageable pageable, Model model){
        loadUsuariosTableAttributes(filters, pageable, model);
        breadcrumbContext.set(breadcrumbService.adminUsuarios());
        return "admin/usuarios/usuarios-list";
    }

    @GetMapping("/tableData")
    public String getUsuariosTableData(@ModelAttribute UsuarioFilterForm filters, Pageable pageable, Model model,
        HttpServletRequest request) {

        String triggeredField = request.getHeader("HX-Trigger-Name");
        
        loadUsuariosTableAttributes(filters, pageable, model);
        
        if ("resetFilters".equals(triggeredField)) {
            return "admin/usuarios/usuarios-list :: table-usuarios-container_fragment";
        }

        return "admin/usuarios/usuarios-list :: table-usuarios_fragment";
    }

    private void loadUsuariosTableAttributes(UsuarioFilterForm filters, Pageable pageable, Model model){
        model.addAttribute("usuariosPage", adminUsuarioService.getUsuarios(filters, pageable));
        model.addAttribute("filters", filters);
        model.addAttribute("rolesDisponibles", Rol.values());
    }

    @GetMapping("/nuevo")
    public String showCreateUsuarioForm(Model model) {
        model.addAttribute("usuarioForm", adminUsuarioService.createUsuarioForm());
        model.addAttribute("generos", generoRepository.findAll());
        model.addAttribute("rolesDisponibles", Rol.values());
        breadcrumbContext.set(breadcrumbService.adminUsuarioNuevo());
        return "admin/usuarios/usuario-form";
    }

    @PostMapping("/nuevo")
    public String createNewUsuario(@Valid @ModelAttribute("usuarioForm") UsuarioForm form,
        BindingResult bindingResult, Model model, RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("generos", generoRepository.findAll());
            model.addAttribute("rolesDisponibles", Rol.values());
            breadcrumbContext.set(breadcrumbService.adminUsuarioNuevo());
            return "admin/usuarios/usuario-form";
        }

        try {
            usuarioService.createUsuario(form);
            toastService.success(ra, "El usuario se ha creado correctamente.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            toastService.error(model, e.getMessage());
            model.addAttribute("generos", generoRepository.findAll());
            model.addAttribute("rolesDisponibles", Rol.values());
            breadcrumbContext.set(breadcrumbService.adminUsuarioNuevo());
            return "admin/usuarios/usuario-form";
        } catch (Exception e){
            toastService.error(model, "Se ha producido un error al crear el usuario.");
            model.addAttribute("generos", generoRepository.findAll());
            model.addAttribute("rolesDisponibles", Rol.values());
            breadcrumbContext.set(breadcrumbService.adminUsuarioNuevo());
            return "admin/usuarios/usuario-form";
        }

        return "redirect:/admin/usuarios";
    }
    
    @GetMapping("{id}/editar")
    public String showEditUsuarioForm(@PathVariable Long id, Model model) {
        Optional<Usuario> usuario = usuarioRepository.findById(id);

        if (!usuario.isPresent()) {
            toastService.error(model, "No se ha encontrado el usuario seleccionado.");
            return "redirect:/admin/usuarios";
        }

        model.addAttribute("usuarioForm", adminUsuarioService.createUsuarioForm(usuario.get()));
        model.addAttribute("generos", generoRepository.findAll());
        model.addAttribute("rolesDisponibles", Rol.values());
        breadcrumbContext.set(breadcrumbService.adminUsuarioEditar(usuario.get().getUsername()));
        return "admin/usuarios/usuario-form";
    }

    @PostMapping("{id}/editar")
    public String editUsuario(@PathVariable Long id, @Valid @ModelAttribute("usuarioForm") UsuarioForm form,
        BindingResult bindingResult, Model model, RedirectAttributes ra) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("generos", generoRepository.findAll());
            model.addAttribute("rolesDisponibles", Rol.values());
            breadcrumbContext.set(breadcrumbService.adminUsuarioEditar(form.getUsername()));
            return "admin/usuarios/usuario-form";
        }

        try {
            usuarioService.updateUsuario(id, form);
            toastService.success(ra, "El usuario se ha actualizado correctamente.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            toastService.error(model, e.getMessage());
            model.addAttribute("generos", generoRepository.findAll());
            model.addAttribute("rolesDisponibles", Rol.values());
            breadcrumbContext.set(breadcrumbService.adminUsuarioEditar(form.getUsername()));
            return "admin/usuarios/usuario-form";
        } catch (Exception e){
            toastService.error(model, "Se ha producido un error al actualizar el usuario.");
            model.addAttribute("generos", generoRepository.findAll());
            model.addAttribute("rolesDisponibles", Rol.values());
            breadcrumbContext.set(breadcrumbService.adminUsuarioEditar(form.getUsername()));
            return "admin/usuarios/usuario-form";
        }

        return "redirect:/admin/usuarios";        
    }

    @PostMapping("{id}/eliminar")
    public String deleteUsuario(@PathVariable Long id, @ModelAttribute UsuarioFilterForm filters, 
        Pageable pageable, Model model, RedirectAttributes ra, HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
            
        try {
            usuarioService.deleteUsuario(id);
            if (hxRequest != null) {
                toastService.success(response, "El usuario se ha eliminado correctamente.");
            } else{
                toastService.success(ra, "El usuario se ha eliminado correctamente.");
            }
        } catch (QuizlyException | EntityNotFoundException e) {
            if (hxRequest != null) toastService.error(response, e.getMessage());
            else toastService.error(ra, e.getMessage());
        } catch (Exception e) {
            if (hxRequest != null) toastService.error(response, "Se ha producido un error al eliminar el usuario.");    
            else toastService.error(ra, "Se ha producido un error al eliminar el usuario.");
        }
        
        return hxRequest != null 
            ? refreshUsuarioTable(filters, pageable, model) 
            : "redirect:/admin/usuarios";
    }

    private String refreshUsuarioTable(UsuarioFilterForm filters, Pageable pageable, Model model){
        loadUsuariosTableAttributes(filters, pageable, model);
        return "admin/usuarios/usuarios-list :: table-usuarios-container_fragment"; 
    }
}
