package com.quizly.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.quizly.app.dto.QuizFilterForm;
import com.quizly.app.model.entity.Cuestionario;

public interface CuestionarioRepositoryCustom {
    
    List<Cuestionario> findByTemaId(Long temaId);

    List<Cuestionario> findByTemaIdAndDificultadId(Long temaId, Long dificultadId);

    boolean existsBySlug(String slug);

    Optional<Cuestionario> findLastVersionBySlug(String slug);

    /**
     * Devuelve los últimos cuestionarios modificados
     * @param limit
     * @return
     */
    List<Cuestionario> findLastModifiedLimit(int limit);

    /**
     * Obtiene los cuestionarios aplicando los filtros indicados
     * y de manera paginada
     * @param filters
     * @param pageable
     * @return
     */
    Page<Cuestionario> findAll(QuizFilterForm filters, Pageable pageable);
}