package com.quizly.app.repository;

import java.util.List;
import java.util.Optional;
import com.quizly.app.model.entity.Cuestionario;

public interface CuestionarioRepositoryCustom {
    
    List<Cuestionario> findByTemaId(Long temaId);

    List<Cuestionario> findByTemaIdAndDificultadId(Long temaId, Long dificultadId);

    boolean existsBySlug(String slug);

    Optional<Cuestionario> findLastVersionBySlug(String slug);
}