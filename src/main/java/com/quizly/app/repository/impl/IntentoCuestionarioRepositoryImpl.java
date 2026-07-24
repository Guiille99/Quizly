package com.quizly.app.repository.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.IntentoCuestionarioEstado;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.repository.IntentoCuestionarioRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

public class IntentoCuestionarioRepositoryImpl implements IntentoCuestionarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<IntentoCuestionario> findByUsuarioId(Long usuarioId) {
        TypedQuery<IntentoCuestionario> query = entityManager.createQuery(
            "SELECT i FROM IntentoCuestionario i WHERE i.usuario.id = :usuarioId", IntentoCuestionario.class);
        query.setParameter("usuarioId", usuarioId);
        return query.getResultList();
    }

    @Override
    public List<IntentoCuestionario> findByCuestionarioId(Long cuestionarioId) {
        TypedQuery<IntentoCuestionario> query = entityManager.createQuery(
            "SELECT i FROM IntentoCuestionario i WHERE i.cuestionario.id = :cuestionarioId", IntentoCuestionario.class);
        query.setParameter("cuestionarioId", cuestionarioId);
        return query.getResultList();
    }

    @Override
    public List<IntentoCuestionario> findByUsuarioIdAndCuestionarioId(Long usuarioId, Long cuestionarioId) {
        TypedQuery<IntentoCuestionario> query = entityManager.createQuery(
            "SELECT i FROM IntentoCuestionario i WHERE i.usuario.id = :usuarioId AND i.cuestionario.id = :cuestionarioId", 
            IntentoCuestionario.class);
        query.setParameter("usuarioId", usuarioId);
        query.setParameter("cuestionarioId", cuestionarioId);
        return query.getResultList();
    }

    @Override
    public List<IntentoCuestionario> getIntentosCuestionariosByEstadoYUsuario(Long usuarioId, String codigoEstado) {
        // Esta consulta es clave: filtra por el código del catálogo de estados
        TypedQuery<IntentoCuestionario> query = entityManager.createQuery(
            "SELECT i FROM IntentoCuestionario i " +
            "JOIN i.estado e " +
            "WHERE i.usuario.id = :usuarioId AND e.codigo = :codigoEstado", 
            IntentoCuestionario.class);
        
        query.setParameter("usuarioId", usuarioId);
        query.setParameter("codigoEstado", codigoEstado);
        return query.getResultList();
    }

    @Override
    public List<IntentoCuestionario> findLatestByUsuarioId(Long usuarioId, int limit) {
        TypedQuery<IntentoCuestionario> query = entityManager.createQuery(
            "SELECT i FROM IntentoCuestionario i WHERE i.usuario.id = :usuarioId ORDER BY i.updatedAt DESC", 
            IntentoCuestionario.class);
        query.setParameter("usuarioId", usuarioId);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public List<IntentoCuestionario> findLatestByUsuarioIdAndEstado(Long usuarioId, List<String> intentosEstadoCodigos,
            int limit) {
        TypedQuery<IntentoCuestionario> query = entityManager.createQuery(
            """
            SELECT i FROM IntentoCuestionario i WHERE i.usuario.id = :usuarioId 
            AND i.estado.codigo IN :intentosEstadoCodigos
            ORDER BY i.updatedAt DESC
            """, 
            IntentoCuestionario.class);
        query.setParameter("usuarioId", usuarioId);
        query.setParameter("intentosEstadoCodigos", intentosEstadoCodigos);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public Double findMaxPorcentajeAntesDe(Long usuarioId, Long cuestionarioId, Long intentoIdActual) {
        TypedQuery<Double> query = entityManager.createQuery(
            "SELECT MAX((CAST(i.aciertos AS double) / (i.aciertos + i.fallos)) * 100) " +
            "FROM IntentoCuestionario i WHERE i.usuario.id = :usuarioId " +
            "AND i.cuestionario.id = :cuestionarioId AND i.id != :intentoId " + 
            "AND i.estado.codigo = :estadoIntentoCodigo", Double.class);
        query.setParameter("usuarioId", usuarioId);
        query.setParameter("cuestionarioId", cuestionarioId);
        query.setParameter("intentoId", intentoIdActual);
        query.setParameter("estadoIntentoCodigo", IntentoCuestionarioEstadoEnum.FINISHED.toString());
        Double result = query.getSingleResult();
        return result != null ? result : -1.0;
    }

    @Override
    public Optional<IntentoCuestionario> findIntentoAbiertoByUsuarioYCuestionario(Long usuarioId, Long cuestionarioId) {
        TypedQuery<IntentoCuestionario> query = entityManager.createQuery("""
            SELECT i FROM IntentoCuestionario i 
            WHERE i.usuario.id = :usuarioId
                AND i.cuestionario.id = :cuestionarioId 
                AND i.estado.codigo = :estadoIntento
            ORDER BY i.fechaInicio DESC
        """, IntentoCuestionario.class);

        query.setParameter("usuarioId", usuarioId);
        query.setParameter("cuestionarioId", cuestionarioId);
        query.setParameter("estadoIntento", IntentoCuestionarioEstadoEnum.IN_PROGRESS.name());

        return query.getResultStream().findFirst();
    }

    @Override
    public Optional<IntentoCuestionario> findIntentoAbiertoByUsuarioYSlug(Long usuarioId, String slug) {
        TypedQuery<IntentoCuestionario> query = entityManager.createQuery("""
            SELECT i FROM IntentoCuestionario i 
            WHERE i.usuario.id = :usuarioId
                AND i.cuestionario.slug = :slug 
                AND i.estado.codigo = :estadoIntento
            ORDER BY i.fechaInicio DESC
        """, IntentoCuestionario.class);

        query.setParameter("usuarioId", usuarioId);
        query.setParameter("slug", slug);
        query.setParameter("estadoIntento", IntentoCuestionarioEstadoEnum.IN_PROGRESS.name());

        return query.getResultStream().findFirst();
    }

    @Override
    public int marcarIntentosAbandonados(Long usuarioId, List<Long> cuestionariosId, IntentoCuestionarioEstado intentoEstado) {
        Query query = entityManager.createQuery("""
            UPDATE IntentoCuestionario i
            SET     i.estado = :intentoEstado,
                    i.fechaFin = :fechaFin,
                    i.tiempoEmpleado = :tiempoEmpleado
            WHERE   i.usuario.id = :usuarioId
                AND i.cuestionario.id IN :cuestionariosId
                AND i.estado.codigo = :estadoAbiertoCodigo
        """);

        query.setParameter("intentoEstado", intentoEstado);
        query.setParameter("fechaFin", LocalDateTime.now());
        query.setParameter("tiempoEmpleado", 0L);
        query.setParameter("usuarioId", usuarioId);
        query.setParameter("cuestionariosId", cuestionariosId);
        query.setParameter("estadoAbiertoCodigo", IntentoCuestionarioEstadoEnum.IN_PROGRESS.toString());

        return query.executeUpdate();
    }

    @Override
    public int actualizarFechaUltimaActividad(Long intentoId, LocalDateTime fecha) {
        return entityManager.createQuery("""
                UPDATE IntentoCuestionario i
                    SET i.fechaUltimaActividad = :fecha
                WHERE i.id = :intentoId
            """)
        .setParameter("fecha", fecha)
        .setParameter("intentoId", intentoId)
        .executeUpdate();
    }
}