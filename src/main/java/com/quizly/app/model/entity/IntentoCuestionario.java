package com.quizly.app.model.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "intentos_cuestionario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntentoCuestionario {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cuestionario_id")
    private Cuestionario cuestionario;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "aciertos", nullable = false)
    private Integer aciertos;

    @Column(name = "fallos", nullable = false)
    private Integer fallos;

    @Column(name = "tiempo_empleado", nullable = false)
    private Long tiempoEmpleado;

    @OneToMany(mappedBy = "intento")
    private List<RespuestaUsuario> respuestas;

    @ManyToOne
    @JoinColumn(name = "estado_id")
    private IntentoCuestionarioEstado estado;

    /**
     * Calcula el porcentaje de aciertos sobre el total de preguntas respondidas.
     */
    public double getPorcentajeAciertos() {
        int total = (aciertos != null ? aciertos : 0) + (fallos != null ? fallos : 0);
        if (total == 0) return 0.0;
        return (aciertos * 100.0) / total;
    }
    
}
