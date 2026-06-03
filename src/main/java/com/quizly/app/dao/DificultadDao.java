package com.quizly.app.dao;

import java.util.Optional;

import com.quizly.app.model.entity.Dificultad;

public interface DificultadDao extends BaseDao<Dificultad, Long> {

    Optional<Dificultad> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
