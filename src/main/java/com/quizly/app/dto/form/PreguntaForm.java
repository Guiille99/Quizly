package com.quizly.app.dto.form;

import java.util.ArrayList;
import java.util.List;

import com.quizly.app.model.entity.TipoPregunta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PreguntaForm {

    @NotBlank(message = "El enunciado es obligatorio")
    @Size(max = 255, message = "El enunciado no puede superar los 255 caracteres")
    private String enunciado;

    @NotNull(message = "El tipo de pregunta es obligatorio")
    private String tipoPreguntaCodigo;

    @Valid
    @Size(min = 2, message = "La pregunta debe tener al menos dos opciones")
    private List<OpcionRespuestaForm> opcionesRespuesta = new ArrayList<>();
}
