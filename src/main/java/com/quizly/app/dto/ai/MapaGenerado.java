package com.quizly.app.dto.ai;

/**
 * Mapa mental devuelto por la IA ya recortado a los límites de la generación.
 *
 * @param titulo    título normalizado del mapa
 * @param contenido árbol en el formato del editor ({@code {"nodeData": {...}}}) pero sin ids: los pone
 *                  {@code MapaMentalService#normalizarContenido} al guardarlo
 * @param nodos     número de nodos del árbol, raíz incluida
 */
public record MapaGenerado(String titulo, String contenido, int nodos) {
}
