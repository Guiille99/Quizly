package com.quizly.app.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemaEstadisticaDto {
    private Long temaId;
    private String nombreTema;
    private Double promedioAciertos;

    public TemaEstadisticaDto(Long temaId, String nombreTema, Double promedioAciertos) {
        this.temaId = temaId;
        this.nombreTema = nombreTema;
        this.promedioAciertos = promedioAciertos;
    }

    public int getPromedioRedondeado(){
        return (int) Math.round(this.promedioAciertos);
    }
}
