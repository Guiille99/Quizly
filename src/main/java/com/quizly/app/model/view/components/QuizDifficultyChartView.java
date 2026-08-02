package com.quizly.app.model.view.components;

import lombok.Builder;

@Builder
public record QuizDifficultyChartView(
    long easy,
    long medium,
    long hard
) {
    public long total(){
        return easy + medium + hard;
    }
}
