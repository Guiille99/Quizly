package com.quizly.app.repository.impl;

import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.repository.CuestionarioRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class CuestionarioRepositoryImpl implements CuestionarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Cuestionario> findByTemaId(Long temaId) {
        TypedQuery<Cuestionario> query = entityManager.createQuery(
            "SELECT c FROM Cuestionario c WHERE c.tema.id = :temaId", Cuestionario.class);
        query.setParameter("temaId", temaId);
        return query.getResultList();
    }

    @Override
    public List<Cuestionario> findByTemaIdAndDificultadId(Long temaId, Long dificultadId) {
        TypedQuery<Cuestionario> query = entityManager.createQuery(
            "SELECT c FROM Cuestionario c WHERE c.tema.id = :temaId AND c.dificultad.id = :dificultadId", 
            Cuestionario.class);
        query.setParameter("temaId", temaId);
        query.setParameter("dificultadId", dificultadId);
        return query.getResultList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(c) FROM Cuestionario c WHERE c.slug = :slug", Long.class)
            .setParameter("slug", slug)
            .getSingleResult();
        return count > 0;
    }

    @Override
    public Optional<Cuestionario> findLastVersionBySlug(String slug) {
        TypedQuery<Cuestionario> query = entityManager.createQuery("""
            SELECT c FROM Cuestionario c WHERE c.slug = :slug ORDER BY c.version DESC
        """, Cuestionario.class);

        query.setParameter("slug", slug);
        return query.getResultStream().findFirst();
    }
}