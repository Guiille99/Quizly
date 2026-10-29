package com.quizly.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/legal")
    public String legal(Model model) {
        // La página identifica al responsable por su nombre: se excluye de los buscadores
        model.addAttribute("noindex", true);
        return "legal";
    }
}
