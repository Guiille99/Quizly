package com.quizly.app.model.view.quiz;

import com.quizly.app.model.entity.Dificultad;

import lombok.Builder;

@Builder
public record QuizListItemView(
    Long id,
    String titulo,
    String tema,
    Dificultad dificultad,
    int numeroPreguntas,
    int version,
    String fechaActualizacion    
) {
}
