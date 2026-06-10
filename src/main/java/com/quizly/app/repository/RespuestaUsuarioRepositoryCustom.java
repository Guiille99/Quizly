package com.quizly.app.repository;

import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.RespuestaUsuario;

public interface RespuestaUsuarioRepositoryCustom {

    List<RespuestaUsuario> findByIntentoId(Long intentoId);

    Optional<RespuestaUsuario> findByIntentoIdAndPreguntaId(Long intentoId, Long preguntaId);
}