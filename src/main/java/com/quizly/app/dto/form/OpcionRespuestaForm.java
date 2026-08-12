package com.quizly.app.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OpcionRespuestaForm {

    @NotBlank(message = "El texto de la opción es obligatorio")
    @Size(max = 255, message = "La opción no puede superar los 255 caracteres")
    private String texto;

    private boolean correcta;

    @Size(max = 500, message = "La retroalimentación no puede superar los 500 caracteres")
    private String retroalimentacion;
}
