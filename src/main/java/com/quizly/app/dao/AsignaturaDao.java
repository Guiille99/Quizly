package com.quizly.app.dao;

import java.util.Optional;

import com.quizly.app.model.entity.Asignatura;

public interface AsignaturaDao extends BaseDao<Asignatura, Long> {

    Optional<Asignatura> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
