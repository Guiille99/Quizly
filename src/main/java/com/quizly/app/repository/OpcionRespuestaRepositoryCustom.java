package com.quizly.app.repository;

import java.util.List;

import com.quizly.app.model.entity.OpcionRespuesta;

public interface OpcionRespuestaRepositoryCustom {
    
    List<OpcionRespuesta> findByPreguntaId(Long preguntaId);

    int deleteByCuestionarioId(Long cuestionarioId);
}