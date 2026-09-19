package com.quizly.app.dto.form;

import java.util.List;

import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Dificultad;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.entity.TipoPregunta;

public record QuizFormOptions(
    List<Asignatura> asignaturas,
    List<Tema> tema,
    List<Dificultad> dificultad,
    List<TipoPregunta> tiposPregunta
) {}
