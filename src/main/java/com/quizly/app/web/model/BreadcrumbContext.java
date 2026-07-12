package com.quizly.app.web.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.quizly.app.model.view.breadcrumb.BreadcrumbItem;

@Component
@RequestScope
public class BreadcrumbContext {
    private final List<BreadcrumbItem> items = new ArrayList<>();

    public void set(List<BreadcrumbItem> items){
        this.items.clear();
        this.items.addAll(items);
    }

    public List<BreadcrumbItem> getItems(){
        return items;
    }
}
