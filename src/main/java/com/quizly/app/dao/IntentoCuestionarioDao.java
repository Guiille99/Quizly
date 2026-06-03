package com.quizly.app.dao;

import java.util.List;

import com.quizly.app.model.entity.IntentoCuestionario;

public interface IntentoCuestionarioDao extends BaseDao<IntentoCuestionario, Long> {

    List<IntentoCuestionario> findByUsuarioId(Long usuarioId);

    List<IntentoCuestionario> findByCuestionarioId(Long cuestionarioId);

    List<IntentoCuestionario> findByUsuarioIdAndCuestionarioId(Long usuarioId, Long cuestionarioId);
}
