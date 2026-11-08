package com.quizly.app.dto.form;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MazoForm {
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    // Solo al editar: las tarjetas existentes llegan como listas paralelas y se guardan junto al mazo
    private List<Long> tarjetaId;
    private List<String> anverso;
    private List<String> reverso;

    // Tarjeta nueva opcional (ambos campos o ninguno)
    private String nuevoAnverso;
    private String nuevoReverso;

    // "Añadir y seguir": tras guardar, el foco vuelve al anverso de la tarjeta nueva
    private boolean seguir;
}
