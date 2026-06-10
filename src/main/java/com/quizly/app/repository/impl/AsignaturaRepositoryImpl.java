package com.quizly.app.repository.impl;

import java.util.Optional;

import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.repository.AsignaturaRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class AsignaturaRepositoryImpl implements AsignaturaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Asignatura> findBySlug(String slug) {
        return entityManager.createQuery("SELECT a FROM Asignatura a WHERE a.slug = :slug", Asignatura.class)
                .setParameter("slug", slug)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsBySlug(String slug) {
        Long count = entityManager.createQuery("SELECT COUNT(a) FROM Asignatura a WHERE a.slug = :slug", Long.class)
                .setParameter("slug", slug)
                .getSingleResult();
        
        return count > 0;
    }
}