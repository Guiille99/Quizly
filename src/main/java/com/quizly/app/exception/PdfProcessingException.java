package com.quizly.app.exception;

/**
 * Excepción lanzada cuando ocurre un error durante la validación 
 * o extracción de texto de un archivo PDF subido por el usuario
 */
public class PdfProcessingException extends RuntimeException {

    public PdfProcessingException(String message){
        super(message);
    }    


    public PdfProcessingException(String message, Throwable cause){
        super(message, cause);
    }
}
