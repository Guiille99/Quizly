package com.quizly.app.repository;

import java.util.Optional;
import com.quizly.app.model.entity.IntentoCuestionarioEstado;

public interface IntentoCuestionarioEstadoRepositoryCustom {
    
    Optional<IntentoCuestionarioEstado> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
}