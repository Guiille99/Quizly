package com.quizly.app.dao;

import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.RespuestaUsuario;

public interface RespuestaUsuarioDao extends BaseDao<RespuestaUsuario, Long> {

    List<RespuestaUsuario> findByIntentoId(Long intentoId);

    Optional<RespuestaUsuario> findByIntentoIdAndPreguntaId(Long intentoId, Long preguntaId);
}
