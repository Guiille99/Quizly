package com.quizly.app.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.IntentoCuestionarioEstado;

public interface IntentoCuestionarioRepositoryCustom {
    
    List<IntentoCuestionario> findByUsuarioId(Long usuarioId);

    List<IntentoCuestionario> findByCuestionarioId(Long cuestionarioId);

    List<IntentoCuestionario> findByUsuarioIdAndCuestionarioId(Long usuarioId, Long cuestionarioId);

    List<IntentoCuestionario> getIntentosCuestionariosByEstadoYUsuario(Long usuarioId, String codigoEstado);

    List<IntentoCuestionario> findLatestByUsuarioId(Long usuarioId, int limit);

    /**
     * Obtiene los N últimos intentos del usuario cuyo código de estado del intento
     * se encuentre en la lista pasada por parámetro
     * @param usuarioId
     * @param intentosEstadoCodigos
     * @param limit
     * @return
     */
    List<IntentoCuestionario> findLatestByUsuarioIdAndEstado(Long usuarioId, List<String> intentosEstadoCodigos, int limit);

    Optional<IntentoCuestionario> findIntentoAbiertoByUsuarioYCuestionario(Long usuarioId, Long cuestionarioId);

    Optional<IntentoCuestionario> findIntentoAbiertoByUsuarioYSlug(Long usuarioId, String slug);

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


    /**
     * Marca como abandonados todos los intentos abiertos
     * de un usuario sobre un cuestionario
     * @param usuarioId
     * @param cuestionariosId
     * @param intentoEstado
     * @return
     */
    int marcarIntentosAbandonados(Long usuarioId, List<Long> cuestionariosId, IntentoCuestionarioEstado intentoEstado);

    /**
     * Actualiza la fecha de última actividad
     * del intento
     * @param intentoId
     * @param fecha
     * @return
     */
    int actualizarFechaUltimaActividad(Long intentoId, LocalDateTime fecha);
   
}