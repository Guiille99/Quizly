package com.quizly.app.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.quizly.app.service.IntentoCuestionarioService;

import lombok.RequiredArgsConstructor;

/**
 * Job programado que cierra automáticamente los intentos de cuestionario
 * cuyo tiempo de inactividad o tiempo límite haya expirado.
 * <p>
 * Su propósito es garantizar que ningún intento quede indefinidamente en estado
 * {@code IN_PROGRESS} únicamente porque el usuario no volvió a interactuar con
 * la aplicación (y por tanto no se desencadenó la comprobación reactiva habitual).
 * <p>
 * La frecuencia de ejecución se configura mediante la propiedad
 * {@code quiz.inactivity-check-cron} del fichero {@code application.yaml}.
 */
@Component
@RequiredArgsConstructor
public class IntentosCaducadosJob {

    private static final Logger log = LoggerFactory.getLogger(IntentosCaducadosJob.class);

    private final IntentoCuestionarioService intentoCuestionarioService;

    /**
     * Tarea programada que se ejecuta periódicamente para cerrar los intentos expirados.
     */
    @Scheduled(cron = "${quiz.inactivity-check-cron:0 0 8 * * *}", zone = "Europe/Madrid")
    public void cerrarIntentosExpirados() {
        log.debug("Iniciando limpieza de intentos de cuestionario expirados...");

        try {
            int cerrados = intentoCuestionarioService.cerrarIntentosExpirados();

            if (cerrados > 0) {
                log.info("Limpieza de intentos expirados completada: {} intento(s) cerrado(s).", cerrados);
            } else {
                log.debug("Limpieza de intentos expirados completada: no se encontraron intentos pendientes.");
            }
        } catch (Exception e) {
            log.error("Error durante la limpieza de intentos de cuestionario expirados.", e);
        }
    }
}

