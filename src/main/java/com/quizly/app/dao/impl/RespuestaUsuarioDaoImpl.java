package com.quizly.app.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.RespuestaUsuarioDao;
import com.quizly.app.model.entity.RespuestaUsuario;

@Repository
public class RespuestaUsuarioDaoImpl extends AbstractDaoImpl<RespuestaUsuario, Long> implements RespuestaUsuarioDao {

    public RespuestaUsuarioDaoImpl() {
        super(RespuestaUsuario.class);
    }

    @Override
    public List<RespuestaUsuario> findByIntentoId(Long intentoId) {
        return entityManager.createQuery(
                        "SELECT r FROM RespuestaUsuario r WHERE r.intento.id = :intentoId",
                        RespuestaUsuario.class)
                .setParameter("intentoId", intentoId)
                .getResultList();
    }

    @Override
    public Optional<RespuestaUsuario> findByIntentoIdAndPreguntaId(Long intentoId, Long preguntaId) {
        return entityManager.createQuery(
                        """
                        SELECT r FROM RespuestaUsuario r
                        WHERE r.intento.id = :intentoId AND r.pregunta.id = :preguntaId
                        """,
                        RespuestaUsuario.class)
                .setParameter("intentoId", intentoId)
                .setParameter("preguntaId", preguntaId)
                .getResultStream()
                .findFirst();
    }
}
