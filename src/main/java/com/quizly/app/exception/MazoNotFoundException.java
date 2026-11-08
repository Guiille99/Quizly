package com.quizly.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** El mazo o la tarjeta no existen o no pertenecen al usuario: se responde 404 en ambos casos. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MazoNotFoundException extends RuntimeException {

    public MazoNotFoundException() {
        super("No existe el mazo o la tarjeta.");
    }
}
