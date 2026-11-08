package com.quizly.app.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tarjetas")
@Getter
@Setter
@NoArgsConstructor
public class Tarjeta extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mazo_id", nullable = false)
    private Mazo mazo;

    @Column(name = "anverso", nullable = false, length = 500)
    private String anverso;

    @Column(name = "reverso", nullable = false, length = 1000)
    private String reverso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id")
    private Pregunta pregunta;

    @Column(name = "repeticiones", nullable = false)
    private int repeticiones = 0;

    @Column(name = "intervalo_dias", nullable = false)
    private int intervaloDias = 0;

    @Column(name = "facilidad", nullable = false, precision = 4, scale = 2)
    private BigDecimal facilidad = new BigDecimal("2.50");

    @Column(name = "proxima_revision", nullable = false)
    private LocalDateTime proximaRevision = LocalDateTime.now();

    @Column(name = "ultima_revision")
    private LocalDateTime ultimaRevision;
}
