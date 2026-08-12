package com.quizly.app.model.enums;

public enum UnidadTiempo {
    SEGUNDOS("Segundos"), MINUTOS("Minutos"), HORAS("Horas");

    private final String nombre;

    UnidadTiempo(String nombre){
        this.nombre = nombre;
    }

    public String getNombre(){
        return nombre;
    }
}
