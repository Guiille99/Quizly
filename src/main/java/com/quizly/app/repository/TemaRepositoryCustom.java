package com.quizly.app.repository;

import java.util.List;
import java.util.Optional;
import com.quizly.app.model.entity.Tema;

public interface TemaRepositoryCustom {
    
    Optional<Tema> findBySlug(String slug);

    List<Tema> findByAsignaturaId(Long asignaturaId);

    boolean existsBySlug(String slug);
}