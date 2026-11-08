package com.quizly.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.quizly.app.model.entity.Tarjeta;
import com.quizly.app.model.enums.Calificacion;

/**
 * Algoritmo SM-2 con granularidad de día (estilo Anki): una tarjeta aprobada vuelve al inicio
 * del día que toque, no a las 24 h exactas. Un "Otra vez" vuelve a los 10 minutos.
 */
@Component
public class Sm2Scheduler {

    static final BigDecimal FACILIDAD_MINIMA = new BigDecimal("1.30");
    static final Duration REPASO_INMEDIATO = Duration.ofMinutes(10);

    public void aplicar(Tarjeta tarjeta, Calificacion calificacion, LocalDateTime ahora) {
        int q = calificacion.getCalidad();

        if (q < 3) {
            tarjeta.setRepeticiones(0);
            tarjeta.setIntervaloDias(1);
            tarjeta.setProximaRevision(ahora.plus(REPASO_INMEDIATO));
        } else {
            int repeticiones = tarjeta.getRepeticiones() + 1;
            int intervalo = switch (repeticiones) {
                case 1 -> 1;
                case 2 -> 6;
                default -> BigDecimal.valueOf(tarjeta.getIntervaloDias())
                        .multiply(tarjeta.getFacilidad())
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            };

            // El intervalo usa la facilidad previa; la nueva solo afecta a los siguientes repasos
            tarjeta.setFacilidad(nuevaFacilidad(tarjeta.getFacilidad(), q));
            tarjeta.setRepeticiones(repeticiones);
            tarjeta.setIntervaloDias(intervalo);
            tarjeta.setProximaRevision(ahora.toLocalDate().plusDays(intervalo).atStartOfDay());
        }

        tarjeta.setUltimaRevision(ahora);
    }

    /**
     * Una pregunta fallada en un cuestionario: mismo reinicio que "Otra vez" pero la tarjeta
     * queda pendiente de inmediato, sin la espera de 10 minutos.
     */
    public void reprogramarPorFallo(Tarjeta tarjeta, LocalDateTime ahora) {
        aplicar(tarjeta, Calificacion.OTRA_VEZ, ahora);
        tarjeta.setProximaRevision(ahora);
    }

    // EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02)), con mínimo 1.3
    private BigDecimal nuevaFacilidad(BigDecimal facilidad, int q) {
        BigDecimal d = BigDecimal.valueOf(5 - q);
        BigDecimal delta = new BigDecimal("0.1")
                .subtract(d.multiply(new BigDecimal("0.08").add(d.multiply(new BigDecimal("0.02")))));
        BigDecimal nueva = facilidad.add(delta).setScale(2, RoundingMode.HALF_UP);
        return nueva.max(FACILIDAD_MINIMA);
    }
}
