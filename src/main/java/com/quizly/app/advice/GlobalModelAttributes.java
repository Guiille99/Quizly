package com.quizly.app.advice;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.quizly.app.model.entity.Usuario;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.util.Constantes;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentUser")
    public Usuario currentUser(@AuthenticationPrincipal QuizlyUserDetails user){
        return user != null ? user.getUsuario() : null;
    }

    @ModelAttribute(Constantes.ATTR_CURRENT_URI)
    public String currentUri(HttpServletRequest request){
        return request.getRequestURI();
    }
}
