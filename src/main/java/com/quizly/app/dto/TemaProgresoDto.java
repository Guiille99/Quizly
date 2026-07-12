package com.quizly.app.dto;

import com.quizly.app.model.entity.Tema;

public record TemaProgresoDto(
    Tema tema,
    int porcentaje) {}
