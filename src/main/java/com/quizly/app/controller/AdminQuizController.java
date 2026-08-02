package com.quizly.app.controller;

import com.quizly.app.service.AdminQuizService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizly.app.dto.QuizFilterForm;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/admin/cuestionarios")
@RequiredArgsConstructor
public class AdminQuizController {

    private final AdminQuizService adminQuizService;

    @GetMapping
    public String showCuestionariosList(@ModelAttribute QuizFilterForm filters, Pageable pageable, Model model) {
        model.addAttribute("cuestionariosPage", adminQuizService.getCuestionarios(filters, pageable));
        model.addAttribute("filters", filters);

        return "admin/cuestionarios/cuestionarios-list";
    }

    @GetMapping("/tableData")
    public String getCuestionariosTableData(@ModelAttribute QuizFilterForm filters, Pageable pageable, Model model) {
        model.addAttribute("cuestionariosPage", adminQuizService.getCuestionarios(filters, pageable));
        model.addAttribute("filters", filters);
        return "admin/cuestionarios/cuestionarios-list :: table-cuestionarios_fragment";
    }
    
}
