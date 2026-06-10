package com.quizly.app.repository;

import java.util.Optional;
import com.quizly.app.model.entity.Dificultad;

public interface DificultadRepositoryCustom {
    
    Optional<Dificultad> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
}