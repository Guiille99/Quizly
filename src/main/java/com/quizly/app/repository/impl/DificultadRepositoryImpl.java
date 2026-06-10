package com.quizly.app.repository.impl;

import java.util.Optional;

import com.quizly.app.model.entity.Dificultad;
import com.quizly.app.repository.DificultadRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class DificultadRepositoryImpl implements DificultadRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Dificultad> findByCodigo(String codigo) {
        return entityManager.createQuery("SELECT d FROM Dificultad d WHERE d.codigo = :codigo", Dificultad.class)
                .setParameter("codigo", codigo)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        Long count = entityManager.createQuery("SELECT COUNT(d) FROM Dificultad d WHERE d.codigo = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
        
        return count > 0;
    }
}