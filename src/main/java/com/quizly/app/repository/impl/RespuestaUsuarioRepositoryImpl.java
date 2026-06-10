package com.quizly.app.repository.impl;

import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.RespuestaUsuario;
import com.quizly.app.repository.RespuestaUsuarioRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class RespuestaUsuarioRepositoryImpl implements RespuestaUsuarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<RespuestaUsuario> findByIntentoId(Long intentoId) {
        TypedQuery<RespuestaUsuario> query = entityManager.createQuery(
            "SELECT r FROM RespuestaUsuario r WHERE r.intento.id = :intentoId", 
            RespuestaUsuario.class);
        query.setParameter("intentoId", intentoId);
        return query.getResultList();
    }

    @Override
    public Optional<RespuestaUsuario> findByIntentoIdAndPreguntaId(Long intentoId, Long preguntaId) {
        TypedQuery<RespuestaUsuario> query = entityManager.createQuery(
            "SELECT r FROM RespuestaUsuario r WHERE r.intento.id = :intentoId AND r.pregunta.id = :preguntaId", 
            RespuestaUsuario.class);
        query.setParameter("intentoId", intentoId);
        query.setParameter("preguntaId", preguntaId);
        
        return query.getResultStream().findFirst();
    }
}