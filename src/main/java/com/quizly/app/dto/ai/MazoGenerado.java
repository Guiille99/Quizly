package com.quizly.app.dto.ai;

import java.util.List;

/** Mazo devuelto por la IA, validado: título normalizado y al menos una tarjeta. */
public record MazoGenerado(String titulo, List<TarjetaGenerada> tarjetas) {
}
