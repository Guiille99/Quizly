package com.quizly.app.repository.impl;

import java.util.Optional;

import com.quizly.app.model.entity.IntentoCuestionarioEstado;
import com.quizly.app.repository.IntentoCuestionarioEstadoRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class IntentoCuestionarioEstadoRepositoryImpl implements IntentoCuestionarioEstadoRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<IntentoCuestionarioEstado> findByCodigo(String codigo) {
        return entityManager.createQuery("SELECT e FROM IntentoCuestionarioEstado e WHERE e.codigo = :codigo", IntentoCuestionarioEstado.class)
                .setParameter("codigo", codigo)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        Long count = entityManager.createQuery("SELECT COUNT(e) FROM IntentoCuestionarioEstado e WHERE e.codigo = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
        
        return count > 0;
    }
}