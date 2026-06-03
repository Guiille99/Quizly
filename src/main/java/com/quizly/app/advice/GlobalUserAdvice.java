package com.quizly.app.advice;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.quizly.app.model.entity.Usuario;
import com.quizly.app.security.QuizlyUserDetails;

@ControllerAdvice
public class GlobalUserAdvice {

    @ModelAttribute("currentUser")
    public Usuario currentUser(@AuthenticationPrincipal QuizlyUserDetails user){
        return user != null ? user.getUsuario() : null;
    }
}
