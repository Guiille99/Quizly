package com.quizly.app.model.view.quiz;

import com.quizly.app.model.entity.Dificultad;

import lombok.Builder;

@Builder
public record QuizView(
    Long id,
    String slug,
    String titulo,
    String tema,
    String asignatura,
    Dificultad dificultad,
    int numeroPreguntas,
    String tiempoLimite,
    int version,
    String fechaActualizacion    
) {
}
