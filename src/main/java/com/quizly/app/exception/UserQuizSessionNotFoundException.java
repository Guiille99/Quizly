package com.quizly.app.exception;

public class UserQuizSessionNotFoundException extends RuntimeException {
    
    public UserQuizSessionNotFoundException(){
        super("No existe una sesión de cuestionario activa.");
    }
}
