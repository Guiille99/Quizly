package com.quizly.app.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.quizly.app.dto.form.ForgotPasswordDto;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.repository.GeneroRepository;
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.security.RateLimiter;
import com.quizly.app.service.AuthService;
import com.quizly.app.service.EmailService;
import com.quizly.app.web.notification.ToastService;

class AuthControllerTest {

    private UsuarioRepository usuarioRepository;
    private EmailService emailService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        emailService = mock(EmailService.class);
        controller = new AuthController(mock(AuthService.class), mock(GeneroRepository.class), mock(ToastService.class),
                usuarioRepository, emailService, "https://quizly.example", new RateLimiter());
    }

    private void usuarioConPassword(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword("hash");
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
    }

    private String forgotPassword(String email) {
        ForgotPasswordDto form = new ForgotPasswordDto();
        form.setEmail(email);
        return controller.processForgotPassword(form, new BeanPropertyBindingResult(form, "forgotPasswordForm"),
                new RedirectAttributesModelMap(), new ExtendedModelMap());
    }

    @Test
    @DisplayName("forgot-password solo envía 3 correos por hora al mismo email y responde igual al resto")
    void forgotPassword_alCuartoIntentoNoEnviaCorreo() throws Exception {
        usuarioConPassword("usuario@example.com");

        for (int i = 0; i < 4; i++) {
            assertThat(forgotPassword("usuario@example.com")).isEqualTo("redirect:/login");
        }

        verify(emailService, times(3)).sendPasswordResetEmail(eq("usuario@example.com"), any(), any());
    }

    @Test
    @DisplayName("El límite de forgot-password es por email")
    void forgotPassword_elLimiteEsPorEmail() throws Exception {
        usuarioConPassword("a@example.com");
        usuarioConPassword("b@example.com");

        for (int i = 0; i < 4; i++) {
            forgotPassword("a@example.com");
        }
        forgotPassword("b@example.com");

        verify(emailService, times(1)).sendPasswordResetEmail(eq("b@example.com"), any(), any());
    }

    @Test
    @DisplayName("El límite de forgot-password ignora mayúsculas y espacios del email")
    void forgotPassword_normalizaElEmail() throws Exception {
        usuarioConPassword("usuario@example.com");

        for (String variante : List.of("Usuario@Example.com", "USUARIO@example.com", " usuario@example.com")) {
            forgotPassword(variante);
        }
        forgotPassword("usuario@example.com");

        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }
}
