package com.quizly.app.exception;

public class QuizNotFoundException extends RuntimeException {

    public QuizNotFoundException() {
        super("No existe el cuestionario.");
    }
}