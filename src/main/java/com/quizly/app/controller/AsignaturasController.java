package com.quizly.app.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.quizly.app.dto.AsignaturaProgresoDto;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.AsignaturaService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quizly.app.util.Constantes;

import jakarta.servlet.http.HttpServletRequest;



@Controller
@RequiredArgsConstructor
public class AsignaturasController {

    private final AsignaturaService asignaturaService;

    @GetMapping("/asignaturas")
    public String showAsignaturas(Model model, HttpServletRequest request,
        @AuthenticationPrincipal QuizlyUserDetails userDetails, Pageable pageable) {
        
        Page<AsignaturaProgresoDto> asignaturasProgreso = asignaturaService.getAsignaturasProgresoUsuarioPaginated(
            userDetails.getUsuario(), null, pageable);

        model.addAttribute("asignaturas", asignaturasProgreso);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        return "asignaturas/asignaturas";
    }
    
    @GetMapping("/asignaturas/elementos")
    public String getAsignaturasElementos(Model model, 
        @AuthenticationPrincipal QuizlyUserDetails userDetails, Pageable pageable,
        @RequestParam(required = false) String nameFilter) {

        Page<AsignaturaProgresoDto> asignaturasProgreso = asignaturaService.getAsignaturasProgresoUsuarioPaginated(
            userDetails.getUsuario(), nameFilter, pageable);

        model.addAttribute("asignaturas", asignaturasProgreso);

        return "asignaturas/asignaturas :: lista-asignaturas_fragment";
    }
    
    
}
