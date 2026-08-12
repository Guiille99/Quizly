package com.quizly.app.model.entity;

import java.util.List;

import com.quizly.app.util.Utils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cuestionarios", uniqueConstraints = {@UniqueConstraint(columnNames = {"slug", "version"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cuestionario extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    /**
     * Tiempo límite del cuestionario en segundos.
     */
    @Column(name = "tiempo_limite")
    private Integer tiempoLimite;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "preguntas_aleatorias", nullable = false)
    private boolean preguntasAleatorias;

    @ManyToOne
    @JoinColumn(name = "tema_id")
    private Tema tema;

    @ManyToOne
    @JoinColumn(name = "dificultad_id")
    private Dificultad dificultad;

    @OneToMany(mappedBy = "cuestionario")
    private List<Pregunta> preguntas;


    @Transient
    public String getTiempoLimiteNatural() {
        if (tiempoLimite == null) {
            return null;
        }
        return Utils.formatNaturalDuration(tiempoLimite);
    }

}
