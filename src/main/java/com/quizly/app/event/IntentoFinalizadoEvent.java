package com.quizly.app.event;

import java.util.List;

import com.quizly.app.model.view.tarjeta.FalloPregunta;

/** Se publica al terminar un intento por el usuario, solo si hubo preguntas no acertadas. */
public record IntentoFinalizadoEvent(Long usuarioId, List<FalloPregunta> fallos) {
}
