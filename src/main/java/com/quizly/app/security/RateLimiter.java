package com.quizly.app.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Limitador de frecuencia por clave con ventanas fijas: la ventana empieza con el primer intento
 * y los contadores se reinician al expirar.
 * <p>
 * shortcut: in-memory, one instance (se pierde al reiniciar y no se comparte entre instancias);
 * migrar a un almacén compartido si la aplicación escala a varias instancias.
 */
@Component
public class RateLimiter {

    // Por encima de este número de claves se descartan las ventanas expiradas para acotar la memoria
    private static final int PURGE_THRESHOLD = 10_000;

    private record Window(Instant start, Duration length, int count) {
        boolean expired(Instant now) {
            return !now.isBefore(start.plus(length));
        }

        Window increment() {
            return new Window(start, length, count + 1);
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Registra un intento solo si no se ha alcanzado el máximo en la ventana actual. */
    public boolean tryAcquire(String key, int max, Duration length) {
        Instant now = clock.instant();
        boolean[] allowed = { false };

        windows.compute(key, (k, w) -> {
            Window current = currentWindow(w, now, length);
            if (current.count() >= max) {
                return current;
            }
            allowed[0] = true;
            return current.increment();
        });

        purgeExpired(now);
        return allowed[0];
    }

    /** Registra un intento sin comprobar ningún máximo. */
    public void hit(String key, Duration length) {
        Instant now = clock.instant();
        windows.compute(key, (k, w) -> currentWindow(w, now, length).increment());
        purgeExpired(now);
    }

    /** Intentos registrados en la ventana actual de la clave. */
    public int count(String key) {
        Window window = windows.get(key);
        return window == null || window.expired(clock.instant()) ? 0 : window.count();
    }

    public void reset(String key) {
        windows.remove(key);
    }

    int size() {
        return windows.size();
    }

    private static Window currentWindow(Window window, Instant now, Duration length) {
        return window == null || window.expired(now) ? new Window(now, length, 0) : window;
    }

    private void purgeExpired(Instant now) {
        if (windows.size() > PURGE_THRESHOLD) {
            windows.values().removeIf(w -> w.expired(now));
        }
    }
}
