package com.quizly.app.dto;

import com.quizly.app.model.entity.Asignatura;

public record AsignaturaProgresoDto(
    Asignatura asignatura, 
    long nTemas, 
    int porcentaje) {}
