package com.quizly.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActividadRecienteDto {
    private String titulo;
    private String subtitulo;
    private String fechaRelativa;
    private String tipo; // COMPLETADO, RECORD, INICIADO
    private String bgColorClass; // Class for background
    private String textColorClass; // Class for text color
}
