package com.quizly.app.mapper;

import org.springframework.stereotype.Component;

import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.view.tema.TemaView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TemaViewMapper {

    public TemaView toTemaView(Tema tema){
        return TemaView.builder()
            .id(tema.getId())
            .nombre(tema.getNombre())
            .slug(tema.getSlug())
            .orden(tema.getOrden())
            .asignatura(tema.getAsignatura().getNombre())
            .nCuestionarios(tema.getCuestionarios().size())
            .build();
    }
}
