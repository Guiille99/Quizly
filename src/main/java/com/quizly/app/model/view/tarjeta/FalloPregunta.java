package com.quizly.app.model.view.tarjeta;

/** Pregunta no acertada en un intento, con el texto ya listo para una tarjeta. */
public record FalloPregunta(Long preguntaId, String anverso, String reverso) {
}
