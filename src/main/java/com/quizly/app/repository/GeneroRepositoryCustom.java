package com.quizly.app.repository;

import java.util.Optional;
import com.quizly.app.model.entity.Genero;

public interface GeneroRepositoryCustom {
    
    Optional<Genero> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
}