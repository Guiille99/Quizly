package com.quizly.app.dto.form;

import com.quizly.app.model.enums.Rol;

import lombok.Data;

@Data 
public class UsuarioFilterForm {
    private String search;
    private Rol rol;
}
