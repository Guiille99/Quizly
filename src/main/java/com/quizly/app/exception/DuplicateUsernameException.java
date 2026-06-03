package com.quizly.app.exception;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException() {
        super("El nombre de usuario ya está en uso");
    }
}
