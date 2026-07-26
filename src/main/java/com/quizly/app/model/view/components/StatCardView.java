package com.quizly.app.model.view.components;

import com.quizly.app.model.view.common.TrendIndicatorView;

import lombok.Builder;

@Builder
public record StatCardView(
    String titulo,
    String subtitulo,
    String icono,
    String color,
    String valor,
    TrendIndicatorView tendencia
) {

}
