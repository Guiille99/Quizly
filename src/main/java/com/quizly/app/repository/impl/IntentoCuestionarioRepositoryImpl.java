package com.quizly.app.repository.impl;

import java.util.List;

import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.repository.IntentoCuestionarioRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
}