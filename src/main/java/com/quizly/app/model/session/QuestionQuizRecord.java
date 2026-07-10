package com.quizly.app.model.session;

import java.util.List;


public record QuestionQuizRecord (
    Long id,
    String statement,
    List<OptionQuizRecord> options
) {}
