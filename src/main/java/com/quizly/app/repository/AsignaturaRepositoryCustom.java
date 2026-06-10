package com.quizly.app.repository;

import java.util.Optional;
import com.quizly.app.model.entity.Asignatura;

public interface AsignaturaRepositoryCustom {
    
    Optional<Asignatura> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
}