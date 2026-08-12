package com.quizly.app.dto.form;

import java.util.List;

import com.quizly.app.model.enums.UnidadTiempo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuizForm {
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título no puede superar los 100 caracteres")
    private String titulo;
    
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    /**
     * Valor número del límite de tiempo introducido por el usuario.
     */
    private Integer tiempoLimite;

    /**
     * Unidad en la que se expresa tiempoLimite en el formulario
     */
    private UnidadTiempo unidadTiempo = UnidadTiempo.MINUTOS;

    /**
     * Indica si el cuestionario tiene límite de tiempo
     */
    private boolean sinLimiteTiempo;

    @NotNull(message = "La asignatura es obligatoria")
    private Long asignaturaId;

    @NotNull(message = "El tema es obligatorio")
    private Long temaId;

    @NotNull(message = "La dificultad es obligatoria")
    private Long dificultadId;

    @Valid
    private List<PreguntaForm> preguntas;

    private boolean preguntasAleatorias;
}
