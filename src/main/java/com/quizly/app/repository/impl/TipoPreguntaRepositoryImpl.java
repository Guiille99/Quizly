package com.quizly.app.repository.impl;

import java.util.Optional;

import com.quizly.app.model.entity.TipoPregunta;
import com.quizly.app.repository.TipoPreguntaRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class TipoPreguntaRepositoryImpl implements TipoPreguntaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<TipoPregunta> findByCodigo(String codigo) {
        return entityManager.createQuery("SELECT tp FROM TipoPregunta tp WHERE tp.codigo = :codigo", TipoPregunta.class)
                .setParameter("codigo", codigo)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        Long count = entityManager.createQuery("SELECT COUNT(tp) FROM TipoPregunta tp WHERE tp.codigo = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
        
        return count > 0;
    }
}