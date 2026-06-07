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

import com.quizly.app.dao.GeneroDao;
import com.quizly.app.dao.UsuarioDao;
import com.quizly.app.dto.ForgotPasswordDto;
import com.quizly.app.dto.RegisterFormDto;
import com.quizly.app.dto.ResetPasswordDto;
import com.quizly.app.exception.DuplicateEmailException;
import com.quizly.app.exception.DuplicateUsernameException;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Genero;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.service.AuthService;
import com.quizly.app.service.EmailService;
import com.quizly.app.web.notification.ToastService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class AuthController {

    private final AuthService authService;
    private final GeneroDao generoDao;
    private final UsuarioDao usuarioDao;
    private final ToastService toastService;
    private final EmailService emailService;

    public AuthController(AuthService authService, GeneroDao generoDao, ToastService toastService, UsuarioDao usuarioDao, EmailService emailService) {
        this.authService = authService;
        this.generoDao = generoDao;
        this.usuarioDao = usuarioDao;
        this.toastService = toastService;
        this.emailService = emailService;
    }

    @ModelAttribute("generos")
    public List<Genero> getGeneros() {
        return generoDao.findAll();
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
                     BindingResult bindingResult, RedirectAttributes ra, Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        
        Optional<Usuario> usuarioOpt = usuarioDao.findByEmail(form.getEmail());

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String token = authService.createPasswordResetToken(usuario);

            try {
                emailService.sendPasswordResetEmail(usuario.getEmail(), token);
            } catch (QuizlyException e) {
                toastService.error(model, e.getMessage());
                return "auth/forgot-password";
            }

            System.out.println("Token generado para " + form.getEmail() + ": " + token);
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
            toastService.error(model, "Se ha producido un error al actualizar la contraseña");
            form.setToken(token);
            return "auth/reset-password";
        }
    }
    
}
