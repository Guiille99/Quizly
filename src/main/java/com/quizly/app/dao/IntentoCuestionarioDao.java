package com.quizly.app.dao;

import java.util.List;

import com.quizly.app.model.entity.IntentoCuestionario;

public interface IntentoCuestionarioDao extends BaseDao<IntentoCuestionario, Long> {

    /**
     * Obtiene todos los cuestionarios realizados por un usuario
     * @param usuarioId
     * @return
     */
    List<IntentoCuestionario> findByUsuarioId(Long usuarioId);

    /**
     * Obtiene todos los intentos realizados sobre un cuestionario en concreto
     * @param cuestionarioId
     * @return
     */
    List<IntentoCuestionario> findByCuestionarioId(Long cuestionarioId);

    /**
     * Obtiene todos los intentos realizados por un usuario sobre un cuestionario en concreto
     * @param usuarioId
     * @param cuestionarioId
     * @return
     */
    List<IntentoCuestionario> findByUsuarioIdAndCuestionarioId(Long usuarioId, Long cuestionarioId);

    /**
     * Obtiene todos los intentos de cuestionarios cuyo estado sea el pasado por parámetro y 
     * realizados por un usuario
     * @param usuarioId
     * @return
     */
    List<IntentoCuestionario> getIntentosCuestionariosByEstadoYUsuario(Long usuarioId, String codigoEstado);
}
