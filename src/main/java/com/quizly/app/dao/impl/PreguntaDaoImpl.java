package com.quizly.app.dao.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.PreguntaDao;
import com.quizly.app.model.entity.Pregunta;

@Repository
public class PreguntaDaoImpl extends AbstractDaoImpl<Pregunta, Long> implements PreguntaDao {

    public PreguntaDaoImpl() {
        super(Pregunta.class);
    }

    @Override
    public List<Pregunta> findByCuestionarioId(Long cuestionarioId) {
        return entityManager.createQuery(
                        "SELECT p FROM Pregunta p WHERE p.cuestionario.id = :cuestionarioId",
                        Pregunta.class)
                .setParameter("cuestionarioId", cuestionarioId)
                .getResultList();
    }
}
