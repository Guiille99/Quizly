package com.quizly.app.dto;

import lombok.Builder;

@Builder
public record QuizFilterForm(
    String search,
    Long asignaturaId,
    Long temaId,
    Long dificultadId
) {}
