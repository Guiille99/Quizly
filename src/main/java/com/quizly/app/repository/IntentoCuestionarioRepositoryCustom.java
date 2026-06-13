package com.quizly.app.repository;

import java.util.List;
import com.quizly.app.model.entity.IntentoCuestionario;

public interface IntentoCuestionarioRepositoryCustom {
    
    List<IntentoCuestionario> findByUsuarioId(Long usuarioId);

    List<IntentoCuestionario> findByCuestionarioId(Long cuestionarioId);

    List<IntentoCuestionario> findByUsuarioIdAndCuestionarioId(Long usuarioId, Long cuestionarioId);

    List<IntentoCuestionario> getIntentosCuestionariosByEstadoYUsuario(Long usuarioId, String codigoEstado);

    List<IntentoCuestionario> findLatestByUsuarioId(Long usuarioId, int limit);

    /**
     * Obtiene el mayor porcentaje registrado por el usuario
     * pasado por parámetro en el cuestionario pasado por parámetro,
     * excluyendo el intento actual.
     * @param usuarioId
     * @param cuestionarioId
     * @param intentoIdActual
     * @return
     */
    Double findMaxPorcentajeAntesDe(Long usuarioId, Long cuestionarioId, Long intentoIdActual);
}