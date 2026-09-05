package com.quizly.app.model.view.asignatura;

import lombok.Builder;

@Builder 
public record AsignaturaView(
    Long id,
    String nombre,
    String slug,
    String descripcion,
    int nTemas
) {}
