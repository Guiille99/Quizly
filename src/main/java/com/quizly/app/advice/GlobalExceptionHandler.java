package com.quizly.app.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.exception.UserQuizSessionNotFoundException;
import com.quizly.app.web.notification.ToastService;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final ToastService toastService;

    @ExceptionHandler(UserQuizSessionNotFoundException.class)
    public String handleUserQuizSessionNotFound(UserQuizSessionNotFoundException ex, RedirectAttributes ra){
        toastService.error(ra, ex.getMessage());

        return "redirect:/cuestionarios";
    }
}
