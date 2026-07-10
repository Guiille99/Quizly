package com.quizly.app.model.view.quiz;

public record QuestionSummary(
    String statement,
    boolean correct,
    String selectedOption,
    String correctOption,
    String explanation
) {}
