package com.quizly.app.exception;

/**
 * Excepción lanzada cuando falla la comunicación con la API de IA
 * o la respuesta recibida no cumple con la estructura esperada
 */
public class AiGenerationException extends RuntimeException {

    public AiGenerationException(String message) {
        super(message);
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
