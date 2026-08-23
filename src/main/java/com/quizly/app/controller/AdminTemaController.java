package com.quizly.app.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizly.app.service.AdminTemaService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/admin/temas")
@RequiredArgsConstructor
public class AdminTemaController {
    private final AdminTemaService adminTemaService;

    @GetMapping
    public String showTemasList(Pageable pageable, Model model){
        loadTemasTableAttributes(pageable, model);
        return "admin/temas/temas-list";
    }

    @GetMapping("/tableData")
    public String getTemaTableData(Pageable pageable, Model model) {
        loadTemasTableAttributes(pageable, model);
        return "admin/temas/temas-list :: table-temas-container_fragment";
    }
    

    private void loadTemasTableAttributes(Pageable pageable, Model model){
        model.addAttribute("temasPage", adminTemaService.getTemas(pageable));
    }
}
