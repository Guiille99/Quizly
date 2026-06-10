package com.quizly.app.repository.impl;

import java.util.Optional;

import com.quizly.app.model.entity.Genero;
import com.quizly.app.repository.GeneroRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class GeneroRepositoryImpl implements GeneroRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Genero> findByCodigo(String codigo) {
        return entityManager.createQuery("SELECT g FROM Genero g WHERE g.codigo = :codigo", Genero.class)
                .setParameter("codigo", codigo)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        Long count = entityManager.createQuery("SELECT COUNT(g) FROM Genero g WHERE g.codigo = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
        
        return count > 0;
    }
}