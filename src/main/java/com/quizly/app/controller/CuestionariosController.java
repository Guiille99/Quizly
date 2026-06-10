package com.quizly.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

import com.quizly.app.util.Constantes;

import jakarta.servlet.http.HttpServletRequest;



@Controller
@RequiredArgsConstructor
public class CuestionariosController {

    @GetMapping("/cuestionarios")
    public String showTodosCuestionarios(Model model, HttpServletRequest request) {
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        return "cuestionarios/cuestionarios-todos";
    }
    
}
