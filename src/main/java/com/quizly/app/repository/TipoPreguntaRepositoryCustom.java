package com.quizly.app.repository;

import java.util.Optional;
import com.quizly.app.model.entity.TipoPregunta;

public interface TipoPreguntaRepositoryCustom {
    
    Optional<TipoPregunta> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
}