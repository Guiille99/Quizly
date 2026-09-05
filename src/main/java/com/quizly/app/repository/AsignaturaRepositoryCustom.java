package com.quizly.app.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.quizly.app.dto.form.AsignaturaFilterForm;
import com.quizly.app.model.entity.Asignatura;

public interface AsignaturaRepositoryCustom {
    
    Optional<Asignatura> findBySlug(String slug);
    
    boolean existsBySlug(String slug);

    Page<Asignatura> findAllWithFilters(AsignaturaFilterForm filters, Pageable pageable);
}