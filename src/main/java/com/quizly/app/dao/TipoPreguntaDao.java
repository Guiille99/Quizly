package com.quizly.app.dao;

import java.util.Optional;

import com.quizly.app.model.entity.TipoPregunta;

public interface TipoPreguntaDao extends BaseDao<TipoPregunta, Long> {

    Optional<TipoPregunta> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
