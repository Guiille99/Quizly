package com.quizly.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

class LoginThrottleFilterTest {

    private LoginThrottleFilter filter;
    private LoginAttemptListener listener;

    @BeforeEach
    void setUp() {
        RateLimiter rateLimiter = new RateLimiter();
        filter = new LoginThrottleFilter(rateLimiter);
        listener = new LoginAttemptListener(rateLimiter);
    }

    private void fallos(String username, int veces) {
        for (int i = 0; i < veces; i++) {
            listener.onBadCredentials(new AuthenticationFailureBadCredentialsEvent(
                    new UsernamePasswordAuthenticationToken(username, "mala"), new BadCredentialsException("x")));
        }
    }

    private MockHttpServletRequest request(String method, String path, String username) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setParameter("username", username);
        return request;
    }

    /** Ejecuta el filtro y devuelve true si la petición llegó al resto de la cadena. */
    private boolean pasa(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain.getRequest() != null;
    }

    @Test
    @DisplayName("Con menos de 5 fallos el login pasa")
    void cuatroFallos_noBloquea() throws Exception {
        fallos("ana", 4);

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(pasa(request("POST", "/login", "ana"), response)).isTrue();
        assertThat(response.getRedirectedUrl()).isNull();
    }

    @Test
    @DisplayName("Con 5 fallos el login se redirige a /login?locked sin llegar a la autenticación")
    void cincoFallos_bloquea() throws Exception {
        fallos("ana", 5);

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(pasa(request("POST", "/login", "ana"), response)).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?locked");
    }

    @Test
    @DisplayName("El bloqueo es por usuario e ignora mayúsculas y espacios")
    void bloqueoPorUsuario() throws Exception {
        fallos("Ana", 5);

        assertThat(pasa(request("POST", "/login", " ana "), new MockHttpServletResponse())).isFalse();
        assertThat(pasa(request("POST", "/login", "luis"), new MockHttpServletResponse())).isTrue();
    }

    @Test
    @DisplayName("Un login correcto reinicia el contador de fallos")
    void loginCorrecto_reiniciaContador() throws Exception {
        fallos("ana", 4);
        listener.onSuccess(new AuthenticationSuccessEvent(
                new UsernamePasswordAuthenticationToken("ana", "ok", List.of())));
        fallos("ana", 4);

        assertThat(pasa(request("POST", "/login", "ana"), new MockHttpServletResponse())).isTrue();
    }

    @Test
    @DisplayName("Solo se filtra el POST de /login")
    void soloFiltraElPostDeLogin() throws Exception {
        fallos("ana", 5);

        assertThat(pasa(request("GET", "/login", "ana"), new MockHttpServletResponse())).isTrue();
        assertThat(pasa(request("POST", "/mi-perfil", "ana"), new MockHttpServletResponse())).isTrue();
    }
}
