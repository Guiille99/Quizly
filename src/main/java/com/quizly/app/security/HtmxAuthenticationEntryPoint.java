package com.quizly.app.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Punto de entrada para peticiones sin sesión (p. ej. tras caducar). En peticiones htmx un 302 a /login haría que
 * htmx pintara la página de login dentro del fragmento; con {@code HX-Redirect} htmx navega al login a pantalla
 * completa. El resto de peticiones se redirigen a /login como siempre.
 * <p>
 * Sustituye a los puntos de entrada de formLogin y oauth2Login, que comparten la misma página de login.
 */
public class HtmxAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final LoginUrlAuthenticationEntryPoint loginRedirect = new LoginUrlAuthenticationEntryPoint("/login");

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        if (request.getHeader("HX-Request") == null) {
            loginRedirect.commence(request, response, authException);
            return;
        }

        response.setHeader("HX-Redirect", request.getContextPath() + "/login");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
