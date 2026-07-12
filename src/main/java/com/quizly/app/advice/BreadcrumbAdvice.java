package com.quizly.app.advice;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.quizly.app.model.view.breadcrumb.BreadcrumbItem;
import com.quizly.app.web.model.BreadcrumbContext;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class BreadcrumbAdvice {
    private final BreadcrumbContext context;

    @ModelAttribute("breadcrumbs")
    public List<BreadcrumbItem> breadcrumbs(){
        return context.getItems();
    }
}
