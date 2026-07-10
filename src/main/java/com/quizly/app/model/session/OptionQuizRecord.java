package com.quizly.app.model.session;

public record OptionQuizRecord(
    Long id,
    String text,
    boolean correct,
    String explanation
) {}
