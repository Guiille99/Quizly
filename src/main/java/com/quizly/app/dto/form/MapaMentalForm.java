package com.quizly.app.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MapaMentalForm {
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    // Opcional: los temas son globales, así que el servicio solo comprueba que exista
    private Long temaId;

    // Solo al editar: árbol del mapa en JSON tal como lo devuelve el editor; el servidor lo revalida entero
    private String contenido;
}
