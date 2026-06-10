package com.quizly.app.repository.impl;

import java.util.List;

import com.quizly.app.model.entity.OpcionRespuesta;
import com.quizly.app.repository.OpcionRespuestaRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class OpcionRespuestaRepositoryImpl implements OpcionRespuestaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<OpcionRespuesta> findByPreguntaId(Long preguntaId) {
        TypedQuery<OpcionRespuesta> query = entityManager.createQuery(
            "SELECT o FROM OpcionRespuesta o WHERE o.pregunta.id = :preguntaId", OpcionRespuesta.class);
        query.setParameter("preguntaId", preguntaId);
        return query.getResultList();
    }
}