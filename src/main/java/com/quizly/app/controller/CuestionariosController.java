package com.quizly.app.controller;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.CuestionarioService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

import com.quizly.app.util.Constantes;

import jakarta.servlet.http.HttpServletRequest;





@Controller
@RequiredArgsConstructor
public class CuestionariosController {

    private final CuestionarioService cuestionarioService;


    @GetMapping("/cuestionarios")
    public String showTodosCuestionarios(Model model, HttpServletRequest request, 
            @PageableDefault(size = 5) Pageable pageable) {
        
        Page<Cuestionario> cuestionarios = cuestionarioService.getCuestionariosPaginated(pageable);

        model.addAttribute("cuestionariosPage", cuestionarios);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        return "cuestionarios/cuestionarios-todos";
    }

    @GetMapping("/cuestionarios/recientes")
    public String showCuestionariosRecientes(Model model, HttpServletRequest request,
           @AuthenticationPrincipal QuizlyUserDetails authUser, 
           @PageableDefault(size = 10) Pageable pageable) {
        
        Page<Cuestionario> cuestionariosRecientes = cuestionarioService.getCuestionariosRecientesPaginated(pageable, authUser);
        
        model.addAttribute("cuestionariosRecientesPage", cuestionariosRecientes);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        return "cuestionarios/cuestionarios-recientes";
    }
    
 
    
    @GetMapping("/cuestionarios/table-data")
    public String getCuestionariosDatosTabla(Model model, 
        @PageableDefault(size = 5) Pageable pageable) {
        
        Page<Cuestionario> cuestionarios = cuestionarioService.getCuestionariosPaginated(pageable);

        model.addAttribute("cuestionariosPage", cuestionarios);

        return "cuestionarios/cuestionarios-todos :: lista-cuestionarios_fragment";
    }

    @GetMapping("/cuestionarios/recientes/table-data")
    public String getCuestionariosRecientesDatosTabla(Model model, @AuthenticationPrincipal QuizlyUserDetails authUser,
        @PageableDefault(size = 10) Pageable pageable) {
        
        Page<Cuestionario> cuestionariosRecientes = cuestionarioService.getCuestionariosRecientesPaginated(pageable, authUser);

        model.addAttribute("cuestionariosRecientesPage", cuestionariosRecientes);

        return "cuestionarios/cuestionarios-recientes :: lista-cuestionarios_fragment";
    }
    
}
