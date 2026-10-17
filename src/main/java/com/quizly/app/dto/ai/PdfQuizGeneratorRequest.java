package com.quizly.app.dto.ai;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que encapsula los parámetros del formulario de generación por PDF.
*/
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class PdfQuizGeneratorRequest {

    @NotNull(message = "Debe proporcionar un archivo PDF.")
    private MultipartFile pdfFile;

    @Min(value = 3, message = "El número mínimo de preguntas es 3.")
    @Max(value = 20, message = "El número máximo de preguntas es 20.")
    @Builder.Default
    private Integer numPreguntas = 5;

    private Long dificultadId;

    private String indicacionesAdicionales;

    private String tipoPreguntas;
}
