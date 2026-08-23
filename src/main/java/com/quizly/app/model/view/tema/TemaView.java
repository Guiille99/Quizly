package com.quizly.app.model.view.tema;

import lombok.Builder;

@Builder
public record TemaView(
    Long id,
    String nombre,
    String slug,
    Integer orden,
    String asignatura,
    int nCuestionarios
) {}
