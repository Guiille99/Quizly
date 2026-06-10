package com.quizly.app.repository;

import java.util.List;
import com.quizly.app.model.entity.Pregunta;

public interface PreguntaRepositoryCustom {
    
    List<Pregunta> findByCuestionarioId(Long cuestionarioId);
}