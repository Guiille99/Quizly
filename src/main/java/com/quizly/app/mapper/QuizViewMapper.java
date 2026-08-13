package com.quizly.app.mapper;

import org.springframework.stereotype.Component;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.view.quiz.QuizView;
import com.quizly.app.util.Utils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuizViewMapper {
    private final Utils utils;
    
    public QuizView toQuizView(Cuestionario cuestionario){
        return toQuizView(cuestionario, true);
    }

    public QuizView toQuizView(Cuestionario cuestionario, boolean ultimaVersion) {
        return QuizView.builder()
                .id(cuestionario.getId())
                .slug(cuestionario.getSlug())
                .titulo(cuestionario.getTitulo())
                .tema(cuestionario.getTema().getNombre())
                .asignatura(cuestionario.getTema().getAsignatura().getNombre())
                .dificultad(cuestionario.getDificultad())
                .numeroPreguntas(cuestionario.getPreguntas().size())
                .tiempoLimite(cuestionario.getTiempoLimiteNatural())
                .version(cuestionario.getVersion())
                .fechaActualizacion(utils.formatFechaRelativa(cuestionario.getUpdatedAt()))
                .ultimaVersion(ultimaVersion)
                .build();
    }
}
