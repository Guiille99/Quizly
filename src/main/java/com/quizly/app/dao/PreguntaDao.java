package com.quizly.app.dao;

import java.util.List;

import com.quizly.app.model.entity.Pregunta;

public interface PreguntaDao extends BaseDao<Pregunta, Long> {

    List<Pregunta> findByCuestionarioId(Long cuestionarioId);
}
