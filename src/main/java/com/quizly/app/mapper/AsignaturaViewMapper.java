package com.quizly.app.mapper;

import org.springframework.stereotype.Component;

import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.view.asignatura.AsignaturaView;

@Component 
public class AsignaturaViewMapper {

    public AsignaturaView toAsignaturaView(Asignatura asignatura){
        return AsignaturaView.builder()
            .id(asignatura.getId())
            .nombre(asignatura.getNombre())
            .slug(asignatura.getSlug())
            .descripcion(asignatura.getDescripcion())
            .nTemas(asignatura.getTemas() != null ? asignatura.getTemas().size() : 0)
            .build();
    }
}
