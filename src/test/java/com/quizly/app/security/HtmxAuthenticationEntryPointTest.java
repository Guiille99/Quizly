package com.quizly.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class HtmxAuthenticationEntryPointTest {

    private final HtmxAuthenticationEntryPoint entryPoint = new HtmxAuthenticationEntryPoint();

    private MockHttpServletResponse commence(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(request, response, new InsufficientAuthenticationException("sin sesión"));
        return response;
    }

    private MockHttpServletRequest htmx(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.addHeader("HX-Request", "true");
        return request;
    }

    @Test
    @DisplayName("Una petición htmx sin sesión recibe 401 y HX-Redirect al login, no un 302")
    void redirigeConHxRedirect() throws Exception {
        MockHttpServletResponse response = commence(htmx("/notificaciones/dropdown"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("HX-Redirect")).isEqualTo("/login");
        assertThat(response.getRedirectedUrl()).isNull();
    }

    @Test
    @DisplayName("HX-Redirect respeta el context path de la aplicación")
    void respetaContextPath() throws Exception {
        MockHttpServletRequest request = htmx("/quizly/notificaciones/dropdown");
        request.setContextPath("/quizly");

        assertThat(commence(request).getHeader("HX-Redirect")).isEqualTo("/quizly/login");
    }

    @Test
    @DisplayName("Una petición normal sin sesión sigue redirigiéndose a /login con un 302")
    void peticionNormalRedirigeAlLogin() throws Exception {
        MockHttpServletResponse response = commence(new MockHttpServletRequest("GET", "/notificaciones/dropdown"));

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).endsWith("/login");
        assertThat(response.getHeader("HX-Redirect")).isNull();
    }
}
