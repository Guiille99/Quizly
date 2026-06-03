package com.quizly.app.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "respuesta_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaUsuario {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean correcta;

    @ManyToOne
    @JoinColumn(name = "intento_id")
    private IntentoCuestionario intento;

    @ManyToOne
    @JoinColumn(name = "pregunta_id")
    private Pregunta pregunta;

    @ManyToOne
    @JoinColumn(name = "opcion_id")
    private OpcionRespuesta opcionSeleccionada;
}
