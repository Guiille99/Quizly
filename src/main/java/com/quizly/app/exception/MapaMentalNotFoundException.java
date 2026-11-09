package com.quizly.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** El mapa mental no existe o no pertenece al usuario: se responde 404 en ambos casos. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MapaMentalNotFoundException extends RuntimeException {

    public MapaMentalNotFoundException() {
        super("No existe el mapa mental.");
    }
}
