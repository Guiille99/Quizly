package com.quizly.app.dto.form;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TemaForm {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    @NotBlank(message = "El nombre es obligatorio")
    private Integer orden;

    @NotNull(message = "La asignatura es obligatoria")
    private Long asignaturaId;

    private boolean editable = true;
}
