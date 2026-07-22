package com.quizly.app.model.entity;

import java.time.Duration;
import java.time.LocalDateTime;
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
public class IntentoCuestionario extends AuditableEntity {
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

    @Column(name = "fecha_fin")
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
     * Calcula el porcentaje de aciertos sobre el total de preguntas del cuestionario.
     */
    public double getPorcentajeAciertos() {
        int total = cuestionario.getPreguntas().size();
        if (total == 0) return 0.0;
        return (aciertos * 100.0) / total;
    }

    public int getPorcentajeAciertosRedondeado(){
        return (int) Math.round(getPorcentajeAciertos());
    }

    public String getFechaFinFormateadaDDMMYYY(){
        return Utils.formatFecha(fechaFin, "dd/MM/yyyy");
    }

    public String getTiempoEmpleadoFormateado() {

        if (tiempoEmpleado == null) {
            return "--:--";
        }
    
        long horas = tiempoEmpleado / 3600;
        long minutos = (tiempoEmpleado % 3600) / 60;
        long segundos = tiempoEmpleado % 60;
    
        if (horas > 0) {
            return String.format("%02d:%02d:%02d", horas, minutos, segundos);
        }
    
        return String.format("%02d:%02d", minutos, segundos);
    }

    /**
     * Devuelve la fecha y hora en la que expira el intento.
     * Si el cuestionario no tiene límite de tiempo devuelve null.
     * @return
     */
    public LocalDateTime getFechaExpiracion(){
        if (cuestionario == null || cuestionario.getTiempoLimite() == null) {
            return null;
        }

        return fechaInicio.plusSeconds(cuestionario.getTiempoLimite());
    }

    /**
     * Indica si el tiempo del cuestionario ha expirado
     * @return
     */
    public boolean isTiempoAgotado(){
        LocalDateTime fechaExpiracion = getFechaExpiracion();

        return fechaExpiracion != null && LocalDateTime.now().isAfter(fechaExpiracion);
    }

    /**
     * Devuelve los segundos restantes.
     * Si no hay límite de tiempo devuelve -1.
     * @return
     */
    public long getSegundosRestantes(){
        LocalDateTime fechaExpiracion = getFechaExpiracion();

        if (fechaExpiracion == null) {
            return -1;
        }

        long segundos = Duration.between(LocalDateTime.now(), fechaExpiracion).getSeconds();

        return Math.max(segundos, 0);
    }
    
}
