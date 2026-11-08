package com.quizly.app.model.view.tarjeta;

/**
 * @param mazoId        mazo de la serie, o {@code null} si no se creó porque ninguna pregunta tiene respuesta correcta
 * @param importadas    tarjetas nuevas creadas
 * @param sinRespuesta  preguntas omitidas por no tener ninguna opción correcta
 * @param yaExistentes  preguntas omitidas porque su anverso ya estaba en el mazo
 */
public record ResultadoImportacion(Long mazoId, int importadas, int sinRespuesta, int yaExistentes) {
}
