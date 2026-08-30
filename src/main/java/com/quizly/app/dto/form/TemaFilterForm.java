package com.quizly.app.dto.form;

import lombok.Builder;

@Builder(toBuilder = true)
public record TemaFilterForm(
    String search,
    Long asignaturaId
) {
    public TemaFilterForm sinAsignatura(){
        return this.toBuilder().asignaturaId(null).build();
    }
}
