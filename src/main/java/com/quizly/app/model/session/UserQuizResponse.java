package com.quizly.app.model.session;

public record UserQuizResponse (
    Long preguntaId,
    Long opcionSeleccionadaId,
    String opcionSeleccionadaTexto,
    boolean correcta
) {}
