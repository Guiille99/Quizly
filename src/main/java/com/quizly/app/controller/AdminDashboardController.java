package com.quizly.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizly.app.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final com.quizly.app.web.model.BreadcrumbContext breadcrumbContext;
    private final com.quizly.app.service.BreadcrumbService breadcrumbService;

    @GetMapping
    public String showDashboard(Model model) {
        model.addAttribute("dashboard", dashboardService.getDashboard());
        breadcrumbContext.set(breadcrumbService.adminDashboard());

        return "admin/dashboard";
    }    
    
}
