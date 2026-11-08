package com.quizly.app.model.enums;

/**
 * Autoevaluación del usuario al repasar una tarjeta. La calidad sigue la escala SM-2 (0-5).
 */
public enum Calificacion {
    OTRA_VEZ(0),
    DIFICIL(3),
    BIEN(4),
    FACIL(5);

    private final int calidad;

    Calificacion(int calidad) {
        this.calidad = calidad;
    }

    public int getCalidad() {
        return calidad;
    }
}
