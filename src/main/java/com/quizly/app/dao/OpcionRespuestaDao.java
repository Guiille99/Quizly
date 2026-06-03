package com.quizly.app.dao;

import java.util.List;

import com.quizly.app.model.entity.OpcionRespuesta;

public interface OpcionRespuestaDao extends BaseDao<OpcionRespuesta, Long> {

    List<OpcionRespuesta> findByPreguntaId(Long preguntaId);
}
