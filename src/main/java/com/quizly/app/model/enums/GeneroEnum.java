package com.quizly.app.model.enums;

import lombok.Getter;

@Getter
public enum GeneroEnum {
    HOMBRE("M"),
    MUJER("F"),
    PREFIERO_NO_DECIRLO("PND");

    private final String code;

    GeneroEnum(String code){
        this.code = code;
    }
}
