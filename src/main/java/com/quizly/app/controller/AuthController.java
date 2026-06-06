package com.quizly.app.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.dao.GeneroDao;
import com.quizly.app.dto.RegisterFormDto;
import com.quizly.app.exception.DuplicateEmailException;
import com.quizly.app.exception.DuplicateUsernameException;
import com.quizly.app.model.entity.Genero;
import com.quizly.app.service.AuthService;
import com.quizly.app.web.notification.ToastService;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final AuthService authService;
    private final GeneroDao generoDao;
    private final ToastService toastService;

    public AuthController(AuthService authService, GeneroDao generoDao, ToastService toastService) {
        this.authService = authService;
        this.generoDao = generoDao;
        this.toastService = toastService;
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
}
