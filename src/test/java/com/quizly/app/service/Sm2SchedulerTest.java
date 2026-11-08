package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.quizly.app.model.entity.Tarjeta;
import com.quizly.app.model.enums.Calificacion;

class Sm2SchedulerTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 8, 10, 30);

    private final Sm2Scheduler scheduler = new Sm2Scheduler();

    private Tarjeta tarjeta(int repeticiones, int intervalo, String facilidad) {
        Tarjeta t = new Tarjeta();
        t.setRepeticiones(repeticiones);
        t.setIntervaloDias(intervalo);
        t.setFacilidad(new BigDecimal(facilidad));
        return t;
    }

    @Test
    @DisplayName("Primera revisión correcta: intervalo 1 día y vuelve al inicio de ese día")
    void primeraRevision() {
        Tarjeta t = tarjeta(0, 0, "2.50");

        scheduler.aplicar(t, Calificacion.BIEN, AHORA);

        assertEquals(1, t.getRepeticiones());
        assertEquals(1, t.getIntervaloDias());
        assertEquals(LocalDateTime.of(2026, 11, 9, 0, 0), t.getProximaRevision());
        assertEquals(AHORA, t.getUltimaRevision());
        assertEquals(new BigDecimal("2.50"), t.getFacilidad());
    }

    @Test
    @DisplayName("Segunda revisión correcta: intervalo 6 días")
    void segundaRevision() {
        Tarjeta t = tarjeta(1, 1, "2.50");

        scheduler.aplicar(t, Calificacion.BIEN, AHORA);

        assertEquals(2, t.getRepeticiones());
        assertEquals(6, t.getIntervaloDias());
        assertEquals(LocalDateTime.of(2026, 11, 14, 0, 0), t.getProximaRevision());
    }

    @Test
    @DisplayName("A partir de la tercera: intervalo = intervalo previo × facilidad previa, y la facilidad nueva solo afecta después")
    void terceraRevisionUsaFacilidadPrevia() {
        Tarjeta t = tarjeta(2, 6, "2.50");

        scheduler.aplicar(t, Calificacion.FACIL, AHORA);

        assertEquals(3, t.getRepeticiones());
        assertEquals(15, t.getIntervaloDias()); // 6 × 2.50, no 6 × 2.60
        assertEquals(new BigDecimal("2.60"), t.getFacilidad());
    }

    @Test
    @DisplayName("DIFICIL baja la facilidad 0.14")
    void dificilBajaFacilidad() {
        Tarjeta t = tarjeta(2, 6, "2.50");

        scheduler.aplicar(t, Calificacion.DIFICIL, AHORA);

        assertEquals(new BigDecimal("2.36"), t.getFacilidad());
    }

    @Test
    @DisplayName("La facilidad nunca baja de 1.30")
    void facilidadMinima() {
        Tarjeta t = tarjeta(2, 6, "1.30");

        scheduler.aplicar(t, Calificacion.DIFICIL, AHORA);

        assertEquals(new BigDecimal("1.30"), t.getFacilidad());
    }

    @Test
    @DisplayName("OTRA_VEZ reinicia repeticiones, deja la facilidad y vuelve en 10 minutos")
    void otraVez() {
        Tarjeta t = tarjeta(5, 30, "2.20");

        scheduler.aplicar(t, Calificacion.OTRA_VEZ, AHORA);

        assertEquals(0, t.getRepeticiones());
        assertEquals(1, t.getIntervaloDias());
        assertEquals(new BigDecimal("2.20"), t.getFacilidad());
        assertEquals(AHORA.plusMinutes(10), t.getProximaRevision());
        assertEquals(AHORA, t.getUltimaRevision());
    }

    @Test
    @DisplayName("Un fallo en cuestionario deja la tarjeta pendiente de inmediato, no a los 10 minutos")
    void reprogramarPorFallo() {
        Tarjeta t = tarjeta(5, 30, "2.20");

        scheduler.reprogramarPorFallo(t, AHORA);

        assertEquals(0, t.getRepeticiones());
        assertEquals(1, t.getIntervaloDias());
        assertEquals(new BigDecimal("2.20"), t.getFacilidad());
        assertEquals(AHORA, t.getProximaRevision());
    }
}
