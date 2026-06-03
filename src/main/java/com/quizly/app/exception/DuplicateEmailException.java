package com.quizly.app.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("El email ya está registrado");
    }
}
