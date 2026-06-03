package com.quizly.app.dao;

import java.util.Optional;

import com.quizly.app.model.entity.IntentoCuestionarioEstado;

public interface IntentoCuestionarioEstadoDao extends BaseDao<IntentoCuestionarioEstado, Long> {

    Optional<IntentoCuestionarioEstado> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
