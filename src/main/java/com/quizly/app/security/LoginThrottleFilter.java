package com.quizly.app.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rechaza los intentos de login de un usuario que ha acumulado demasiados fallos recientes.
 * Los fallos se cuentan en {@link LoginAttemptListener}. Se limita por nombre de usuario y no por IP porque,
 * tras el proxy, la IP del cliente no es fiable.
 * <p>
 * No es un {@code @Component} a propósito: Spring Boot registraría el filtro también fuera de la cadena de
 * seguridad. Se instancia en {@code SecurityConfig}.
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final RateLimiter rateLimiter;

    public LoginThrottleFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    static String key(String username) {
        return "login:" + username.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String username = request.getParameter("username");

        if (username != null && rateLimiter.count(key(username)) >= MAX_FAILURES) {
            response.sendRedirect(request.getContextPath() + "/login?locked");
            return;
        }

        chain.doFilter(request, response);
    }
}
