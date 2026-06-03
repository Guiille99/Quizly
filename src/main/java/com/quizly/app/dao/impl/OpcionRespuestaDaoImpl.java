package com.quizly.app.dao.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.OpcionRespuestaDao;
import com.quizly.app.model.entity.OpcionRespuesta;

@Repository
public class OpcionRespuestaDaoImpl extends AbstractDaoImpl<OpcionRespuesta, Long> implements OpcionRespuestaDao {

    public OpcionRespuestaDaoImpl() {
        super(OpcionRespuesta.class);
    }

    @Override
    public List<OpcionRespuesta> findByPreguntaId(Long preguntaId) {
        return entityManager.createQuery(
                        "SELECT o FROM OpcionRespuesta o WHERE o.pregunta.id = :preguntaId",
                        OpcionRespuesta.class)
                .setParameter("preguntaId", preguntaId)
                .getResultList();
    }
}
