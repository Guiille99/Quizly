package com.quizly.app.model.view.common;

import com.quizly.app.model.enums.TrendType;

import lombok.Builder;

@Builder
public record TrendIndicatorView(
    TrendType tipo,
    String valor
) {}
