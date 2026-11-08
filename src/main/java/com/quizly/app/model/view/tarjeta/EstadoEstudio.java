package com.quizly.app.model.view.tarjeta;

import com.quizly.app.model.entity.Tarjeta;

/**
 * Situación de una sesión de estudio.
 *
 * @param tarjeta       siguiente tarjeta pendiente, o {@code null} si no hay
 * @param pendientes    tarjetas pendientes ahora mismo (incluye la actual)
 * @param total         tarjetas del mazo
 * @param minutosEspera si no hay pendientes pero alguna vuelve en la ventana de repaso inmediato, minutos hasta ella
 */
public record EstadoEstudio(Tarjeta tarjeta, long pendientes, long total, Long minutosEspera) {
}
