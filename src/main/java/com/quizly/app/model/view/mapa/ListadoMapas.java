package com.quizly.app.model.view.mapa;

import java.util.List;

import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.MapaMental;

/**
 * @param mapas       los mapas del usuario, ya filtrados por asignatura si se pidió
 * @param asignaturas las asignaturas de sus mapas (sin filtrar), para ofrecer el filtro
 * @param asignaturaId la asignatura filtrada, o {@code null} si se ven todos
 */
public record ListadoMapas(List<MapaMental> mapas, List<Asignatura> asignaturas, Long asignaturaId) {
}
