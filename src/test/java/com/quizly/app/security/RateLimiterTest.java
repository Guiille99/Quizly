package com.quizly.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final Duration MINUTO = Duration.ofMinutes(1);

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MutableClock clock;
    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        limiter = new RateLimiter(clock);
    }

    @Test
    @DisplayName("tryAcquire permite hasta el máximo y deniega después sin consumir más")
    void tryAcquire_permiteHastaElMaximo() {
        assertThat(limiter.tryAcquire("k", 2, MINUTO)).isTrue();
        assertThat(limiter.tryAcquire("k", 2, MINUTO)).isTrue();
        assertThat(limiter.tryAcquire("k", 2, MINUTO)).isFalse();
        assertThat(limiter.count("k")).isEqualTo(2);
    }

    @Test
    @DisplayName("tryAcquire vuelve a permitir cuando expira la ventana")
    void tryAcquire_reiniciaTrasExpirarLaVentana() {
        limiter.tryAcquire("k", 1, MINUTO);
        assertThat(limiter.tryAcquire("k", 1, MINUTO)).isFalse();

        clock.advance(MINUTO);

        assertThat(limiter.tryAcquire("k", 1, MINUTO)).isTrue();
    }

    @Test
    @DisplayName("Las claves son independientes")
    void clavesIndependientes() {
        limiter.tryAcquire("a", 1, MINUTO);

        assertThat(limiter.tryAcquire("a", 1, MINUTO)).isFalse();
        assertThat(limiter.tryAcquire("b", 1, MINUTO)).isTrue();
    }

    @Test
    @DisplayName("hit cuenta, count caduca con la ventana y reset borra el contador")
    void hitCountYReset() {
        limiter.hit("k", MINUTO);
        limiter.hit("k", MINUTO);
        limiter.hit("k", MINUTO);
        assertThat(limiter.count("k")).isEqualTo(3);

        clock.advance(MINUTO);
        assertThat(limiter.count("k")).isZero();

        limiter.hit("k", MINUTO);
        limiter.reset("k");
        assertThat(limiter.count("k")).isZero();
    }

    @Test
    @DisplayName("Se descartan las ventanas expiradas cuando hay demasiadas claves")
    void purgaVentanasExpiradas() {
        for (int i = 0; i <= 10_000; i++) {
            limiter.hit("k" + i, MINUTO);
        }
        clock.advance(Duration.ofMinutes(2));

        limiter.hit("nueva", MINUTO);

        assertThat(limiter.size()).isEqualTo(1);
    }
}
