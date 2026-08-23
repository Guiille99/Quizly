package com.quizly.app.repository.impl;

import java.util.List;

import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.repository.PreguntaRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

public class PreguntaRepositoryImpl implements PreguntaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Pregunta> findByCuestionarioId(Long cuestionarioId) {
        TypedQuery<Pregunta> query = entityManager.createQuery(
            "SELECT p FROM Pregunta p WHERE p.cuestionario.id = :cuestionarioId", 
            Pregunta.class
        );
        query.setParameter("cuestionarioId", cuestionarioId);
        return query.getResultList();
    }

    @Override
    public int deleteByCuestionarioId(Long cuestionarioId) {
        Query query = entityManager.createQuery("""
            DELETE FROM Pregunta p
            WHERE p.cuestionario.id = :cuestionarioId
        """);

        query.setParameter("cuestionarioId", cuestionarioId);

        return query.executeUpdate();
    }
}