package com.quizly.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.quizly.app.web.notification.ToastService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class HomeController {
    private final ToastService toastService;

    @GetMapping("/")
    public String home(Model model) {
        // toastService.success(model,"Hello, worldssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss!");
        // toastService.error(model,"Hello, world!!");
        // toastService.error(model,"Hello, world!!!");

        return "index";
    }

   
    
}
