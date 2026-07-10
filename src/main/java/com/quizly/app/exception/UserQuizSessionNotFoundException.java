package com.quizly.app.exception;

public class UserQuizSessionNotFoundException extends RuntimeException {
    
    public UserQuizSessionNotFoundException(String message){
        super(message);
    }
}
