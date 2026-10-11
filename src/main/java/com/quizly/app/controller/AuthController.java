package com.quizly.app.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.quizly.app.repository.GeneroRepository;
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.dto.form.ForgotPasswordDto;
import com.quizly.app.dto.form.RegisterFormDto;
import com.quizly.app.dto.form.ResetPasswordDto;
import com.quizly.app.exception.DuplicateEmailException;
import com.quizly.app.exception.DuplicateUsernameException;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Genero;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.service.AuthService;
import com.quizly.app.service.EmailService;
import com.quizly.app.web.notification.ToastService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
public class AuthController {

    private final AuthService authService;
    private final GeneroRepository generoRepository;
    private final UsuarioRepository usuarioRepository; // Inyección del nuevo repositorio
    private final ToastService toastService;
    private final EmailService emailService;

    public AuthController(AuthService authService, GeneroRepository generoRepository, ToastService toastService, UsuarioRepository usuarioRepository, EmailService emailService) {
        this.authService = authService;
        this.generoRepository = generoRepository;
        this.usuarioRepository = usuarioRepository;
        this.toastService = toastService;
        this.emailService = emailService;
    }

    @ModelAttribute("generos")
    public List<Genero> getGeneros() {
        return generoRepository.findAll();
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterFormDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(Model model, @Valid @ModelAttribute("registerForm") RegisterFormDto form, 
                         BindingResult bindingResult, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(form);
            toastService.success(ra, "¡Registro completado con éxito! Ya puedes iniciar sesión.");
        } catch (DuplicateUsernameException e) {
            bindingResult.rejectValue("username", "username.duplicate", e.getMessage());
            return "auth/register";
        } catch (DuplicateEmailException e) {
            bindingResult.rejectValue("email", "email.duplicate", e.getMessage());
            return "auth/register";
        } catch(Exception e){
            log.error("Se ha producido un error inesperado durante el registro", e);
            toastService.error(model, "Se ha producido un error durante el registro.");
            return "auth/register";
        }

        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordDto());
        }
        return "auth/forgot-password";
    }
    
    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordDto form, 
                     BindingResult bindingResult, RedirectAttributes ra, Model model,
                     HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(form.getEmail()); // Usando el nuevo repositorio

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String token = authService.createPasswordResetToken(usuario);
            String appUrl = ServletUriComponentsBuilder.fromContextPath(request).toUriString();

            try {
                log.info("Solicitud de recuperación de contraseña enviada por el usuario '{}'", usuario.getUsername());
                emailService.sendPasswordResetEmail(usuario.getEmail(), token, appUrl);
            } catch (QuizlyException e) {
                log.error("Se ha producido un error al enviar el email de recuperación de contraseña", e);
                toastService.error(model, e.getMessage());
                return "auth/forgot-password";
            }
        }

        toastService.info(ra, "Si el email está registrado, recibirás un enlace de recuperación pronto.");
        return "redirect:/login";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam("token") String token, Model model){
        ResetPasswordDto resetPasswdDto = new ResetPasswordDto();
        resetPasswdDto.setToken(token);
        model.addAttribute("resetForm", resetPasswdDto);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
        @Valid @ModelAttribute("resetForm") ResetPasswordDto form, 
        BindingResult bindingResult, RedirectAttributes ra, Model model){

        if (bindingResult.hasErrors()) {
            form.setToken(token);
            return "auth/reset-password";
        }

        try {
            authService.resetPassword(token, form.getPassword());
            toastService.success(ra, "Tu contraseña ha sido actualizada con éxito.");
            return "redirect:/login";
        } catch(QuizlyException e){
            toastService.error(model, e.getMessage());
            form.setToken(token);
            return "auth/reset-password";
        } catch (Exception e) {
            log.error("Error inesperado al actualizar la contraseña", e);
            toastService.error(model, "Se ha producido un error al actualizar la contraseña");
            form.setToken(token);
            return "auth/reset-password";
        }
    }
    
}
