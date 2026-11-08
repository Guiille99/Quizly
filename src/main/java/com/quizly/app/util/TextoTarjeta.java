package com.quizly.app.util;

import java.util.List;
import java.util.Locale;

/**
 * Normalización única del texto de las tarjetas: se usa al guardar y al comparar,
 * para que la deduplicación por anverso sea coherente con el texto ya truncado.
 */
public final class TextoTarjeta {

    public static final int MAX_ANVERSO = 500;
    public static final int MAX_REVERSO = 1000;

    private static final String ELIPSIS = "…";

    private TextoTarjeta() {
    }

    /** Recorta, colapsa espacios en blanco y trunca con "…" si supera {@code max}. */
    public static String normalizar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.strip().replaceAll("\\s+", " ");
        if (limpio.length() <= max) {
            return limpio;
        }
        return limpio.substring(0, max - ELIPSIS.length()).stripTrailing() + ELIPSIS;
    }

    /**
     * Reverso de una tarjeta nacida de una pregunta: opción(es) correcta(s) unidas con "; " y,
     * si la hay, la retroalimentación tras " — ". Ya normalizado al máximo del reverso.
     */
    public static String componerReverso(List<String> correctas, String retroalimentacion) {
        String texto = String.join("; ", correctas);
        if (retroalimentacion != null && !retroalimentacion.isBlank()) {
            texto += " — " + retroalimentacion;
        }
        return normalizar(texto, MAX_REVERSO);
    }

    /** Clave de comparación: texto ya normalizado y sin distinguir mayúsculas. */
    public static String clave(String anverso) {
        return normalizar(anverso, MAX_ANVERSO).toLowerCase(Locale.ROOT);
    }
}
