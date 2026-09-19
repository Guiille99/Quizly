package com.quizly.app.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.exception.UserQuizSessionNotFoundException;
import com.quizly.app.web.notification.ToastService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final ToastService toastService;

    @Value("${ai.max-file-size-mb:20}")
    private int maxFileSizeMb;

    @ExceptionHandler(UserQuizSessionNotFoundException.class)
    public String handleUserQuizSessionNotFound(UserQuizSessionNotFoundException ex, RedirectAttributes ra){
        toastService.error(ra, ex.getMessage());

        return "redirect:/cuestionarios";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, RedirectAttributes ra){
        toastService.error(ra, "El archivo subido supera el tamaño máximo permitido (" + maxFileSizeMb + " MB).");
        return "redirect:/mis-cuestionarios";
    }
}
