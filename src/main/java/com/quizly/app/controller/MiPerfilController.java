package com.quizly.app.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.dto.form.CambioPasswordForm;
import com.quizly.app.dto.form.PerfilForm;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.BreadcrumbService;
import com.quizly.app.service.UsuarioService;
import com.quizly.app.web.model.BreadcrumbContext;
import com.quizly.app.web.notification.ToastService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/mi-perfil")
@RequiredArgsConstructor
@Slf4j
public class MiPerfilController {
    private final UsuarioService usuarioService;
    private final ToastService toastService;
    private final BreadcrumbContext breadcrumbContext;
    private final BreadcrumbService breadcrumbService;

    @GetMapping
    public String showProfile(@AuthenticationPrincipal QuizlyUserDetails authUser, Model model) {
        return renderProfile(model, authUser, null, null);
    }

    @PostMapping("/informacion")
    public String updateProfile(@Valid @ModelAttribute("perfilForm") PerfilForm form,
            BindingResult bindingResult, @AuthenticationPrincipal QuizlyUserDetails authUser,
            Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderProfile(model, authUser, form, new CambioPasswordForm());
        }

        try {
            Usuario updatedUser = usuarioService.updatePerfil(authUser.getId(), form);
            refreshPrincipal(authUser, updatedUser);
            log.info("El usuario {} actualizó su información de perfil", authUser.getId());
            toastService.success(redirectAttributes, "Tu información personal se ha actualizado.");
            return "redirect:/mi-perfil";
        } catch (IllegalArgumentException exception) {
            log.warn("No se pudo actualizar el perfil del usuario {}: {}", authUser.getId(), exception.getMessage());
            toastService.error(model, exception.getMessage());
            return renderProfile(model, authUser, form, new CambioPasswordForm());
        }
    }

    @PostMapping("/contrasena")
    public String changePassword(@Valid @ModelAttribute("passwordForm") CambioPasswordForm form,
            BindingResult bindingResult, @AuthenticationPrincipal QuizlyUserDetails authUser,
            Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderProfile(model, authUser, null, form);
        }

        try {
            Usuario updatedUser = usuarioService.changePassword(authUser.getId(), form);
            refreshPrincipal(authUser, updatedUser);
            log.info("El usuario {} cambió su contraseña", authUser.getId());
            toastService.success(redirectAttributes, "Tu contraseña se ha actualizado.");
            return "redirect:/mi-perfil";
        } catch (IllegalArgumentException exception) {
            log.warn("No se pudo cambiar la contraseña del usuario {}: {}", authUser.getId(), exception.getMessage());
            toastService.error(model, exception.getMessage());
            return renderProfile(model, authUser, null, form);
        }
    }

    @PostMapping("/eliminar-cuenta")
    public String deleteAccount(@AuthenticationPrincipal QuizlyUserDetails authUser,
            HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.deleteCuenta(authUser.getId());
            new SecurityContextLogoutHandler().logout(request, response,
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
            log.info("El usuario {} eliminó su cuenta", authUser.getId());
            toastService.success(redirectAttributes, "Tu cuenta se ha eliminado correctamente.");
            return "redirect:/";
        } catch (EntityNotFoundException exception) {
            log.warn("No se encontró la cuenta {} durante la solicitud de eliminación", authUser.getId());
            toastService.error(redirectAttributes, "No se ha encontrado la cuenta que quieres eliminar.");
            return "redirect:/login";
        } catch (RuntimeException exception) {
            log.error("Error inesperado al eliminar la cuenta del usuario {}", authUser.getId(), exception);
            toastService.error(redirectAttributes, "No se pudo eliminar la cuenta. Inténtalo de nuevo más tarde.");
            return "redirect:/mi-perfil";
        }
    }

    private String renderProfile(Model model, QuizlyUserDetails authUser,
            PerfilForm perfilForm, CambioPasswordForm passwordForm) {
        Usuario usuario = usuarioService.getUsuario(authUser.getId());
        if (perfilForm == null) {
            perfilForm = new PerfilForm();
            perfilForm.setNombre(usuario.getNombre());
            perfilForm.setApellidos(usuario.getApellidos());
            perfilForm.setEmail(usuario.getEmail());
            perfilForm.setUsername(usuario.getUsername());
        }
        model.addAttribute("profileUser", usuario);
        model.addAttribute("perfilForm", perfilForm);
        model.addAttribute("passwordForm", passwordForm != null ? passwordForm : new CambioPasswordForm());
        breadcrumbContext.set(breadcrumbService.miPerfil());
        return "perfil/mi-perfil";
    }

    private void refreshPrincipal(QuizlyUserDetails authUser, Usuario updatedUser) {
        Usuario principalUser = authUser.getUsuario();
        principalUser.setNombre(updatedUser.getNombre());
        principalUser.setApellidos(updatedUser.getApellidos());
        principalUser.setUsername(updatedUser.getUsername());
        principalUser.setEmail(updatedUser.getEmail());
        principalUser.setPassword(updatedUser.getPassword());
        principalUser.setUpdatedAt(updatedUser.getUpdatedAt());
    }
}