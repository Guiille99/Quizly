package com.quizly.app.dao;

import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.Tema;

public interface TemaDao extends BaseDao<Tema, Long> {

    Optional<Tema> findBySlug(String slug);

    List<Tema> findByAsignaturaId(Long asignaturaId);

    boolean existsBySlug(String slug);
}
