package com.quizly.app.repository;

import java.util.List;
import com.quizly.app.model.entity.IntentoCuestionario;

public interface IntentoCuestionarioRepositoryCustom {
    
    List<IntentoCuestionario> findByUsuarioId(Long usuarioId);

    List<IntentoCuestionario> findByCuestionarioId(Long cuestionarioId);

    List<IntentoCuestionario> findByUsuarioIdAndCuestionarioId(Long usuarioId, Long cuestionarioId);

    List<IntentoCuestionario> getIntentosCuestionariosByEstadoYUsuario(Long usuarioId, String codigoEstado);
}