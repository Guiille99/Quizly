package com.quizly.app.dao;

import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.Cuestionario;

public interface CuestionarioDao extends BaseDao<Cuestionario, Long> {

    Optional<Cuestionario> findBySlug(String slug);

    List<Cuestionario> findByTemaId(Long temaId);

    List<Cuestionario> findByTemaIdAndDificultadId(Long temaId, Long dificultadId);

    boolean existsBySlug(String slug);
}
