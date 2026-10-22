package com.quizly.app.model.entity;

import java.time.LocalDateTime;

import com.quizly.app.model.enums.EstadoComparticion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "cuestionarios_compartidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CuestionarioCompartido extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuestionario_origen_id", nullable = false)
    private Cuestionario cuestionarioOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuestionario_clonado_id")
    private Cuestionario cuestionarioClonado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoComparticion estado;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}

