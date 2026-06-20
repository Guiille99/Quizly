package com.quizly.app.controller;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.service.CuestionarioService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
 
    
    @GetMapping("/cuestionarios/table-data")
    public String getCuestionariosDatosTabla(Model model, 
        @PageableDefault(size = 5) Pageable pageable) {
        
        Page<Cuestionario> cuestionarios = cuestionarioService.getCuestionariosPaginated(pageable);

        model.addAttribute("cuestionariosPage", cuestionarios);

        return "cuestionarios/cuestionarios-todos :: lista-cuestionarios_fragment";
    }
    
}
