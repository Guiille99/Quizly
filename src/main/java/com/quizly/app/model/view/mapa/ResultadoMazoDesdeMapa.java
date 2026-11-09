package com.quizly.app.model.view.mapa;

/**
 * @param mazoId  mazo creado, o {@code null} si el mapa no tiene ramas y por tanto no hay tarjetas que crear
 * @param creadas tarjetas guardadas en el mazo
 * @param total   tarjetas que daba el mapa; si supera a {@code creadas}, el resto se omitió por el máximo por mazo
 */
public record ResultadoMazoDesdeMapa(Long mazoId, int creadas, int total) {
}
