package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quizly.app.dto.form.CambioPasswordForm;
import com.quizly.app.dto.form.PerfilForm;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.GeneroRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.repository.OpcionRespuestaRepository;
import com.quizly.app.repository.PasswordResetTokenRepository;
import com.quizly.app.repository.PreguntaRepository;
import com.quizly.app.repository.RespuestaUsuarioRepository;
import com.quizly.app.repository.UsuarioRepository;

import static org.mockito.Mockito.mock;

class UsuarioPerfilServiceTest {
    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private CuestionarioRepository cuestionarioRepository;
    private IntentoCuestionarioRepository intentoRepository;
    private RespuestaUsuarioRepository respuestaRepository;
    private PasswordResetTokenRepository tokenRepository;
    private OpcionRespuestaRepository opcionRepository;
    private PreguntaRepository preguntaRepository;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        cuestionarioRepository = mock(CuestionarioRepository.class);
        intentoRepository = mock(IntentoCuestionarioRepository.class);
        respuestaRepository = mock(RespuestaUsuarioRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        opcionRepository = mock(OpcionRespuestaRepository.class);
        preguntaRepository = mock(PreguntaRepository.class);
        usuarioService = new UsuarioService(
            usuarioRepository,
            mock(GeneroRepository.class),
            passwordEncoder,
            cuestionarioRepository,
            intentoRepository,
            respuestaRepository,
            tokenRepository,
            opcionRepository,
            preguntaRepository
        );
    }

    @Test
    void updatePerfilNormalizesFieldsAndKeepsAccountIdScope() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(usuarioRepository.existsByUsernameIgnoreCaseAndIdNot("ana", 7L)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("ana@example.com", 7L)).thenReturn(false);

        PerfilForm form = new PerfilForm();
        form.setNombre(" Ana ");
        form.setApellidos(" Pérez ");
        form.setUsername(" ana ");
        form.setEmail(" ana@example.com ");

        Usuario updated = usuarioService.updatePerfil(7L, form);

        assertEquals("Ana", updated.getNombre());
        assertEquals("Pérez", updated.getApellidos());
        assertEquals("ana", updated.getUsername());
        assertEquals("ana@example.com", updated.getEmail());
        verify(usuarioRepository).existsByUsernameIgnoreCaseAndIdNot("ana", 7L);
    }

    @Test
    void updatePerfilRejectsAnEmailUsedByAnotherAccount() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByUsernameIgnoreCaseAndIdNot("ana", 7L)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("ana@example.com", 7L)).thenReturn(true);

        PerfilForm form = new PerfilForm();
        form.setNombre("Ana");
        form.setApellidos("Pérez");
        form.setUsername("ana");
        form.setEmail("ana@example.com");

        assertThrows(IllegalArgumentException.class, () -> usuarioService.updatePerfil(7L, form));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void changePasswordRejectsAnIncorrectCurrentPassword() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setPassword("encoded-old");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        CambioPasswordForm form = passwordForm("wrong", "new-password", "new-password");

        assertThrows(IllegalArgumentException.class, () -> usuarioService.changePassword(7L, form));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void changePasswordEncodesTheNewValue() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setPassword("encoded-old");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario updated = usuarioService.changePassword(
            7L, passwordForm("old-password", "new-password", "new-password"));

        assertEquals("encoded-new", updated.getPassword());
    }

    @Test
    void deleteCuentaPreservesQuizzesWithOtherUsersAttempts() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        Cuestionario sharedQuiz = new Cuestionario();
        sharedQuiz.setId(10L);
        sharedQuiz.setUsuario(usuario);
        Cuestionario privateQuiz = new Cuestionario();
        privateQuiz.setId(11L);
        privateQuiz.setUsuario(usuario);

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(cuestionarioRepository.findByUsuario_Id(7L)).thenReturn(List.of(sharedQuiz, privateQuiz));
        when(intentoRepository.existsByCuestionario_IdAndUsuario_IdNot(10L, 7L)).thenReturn(true);
        when(intentoRepository.existsByCuestionario_IdAndUsuario_IdNot(11L, 7L)).thenReturn(false);

        usuarioService.deleteCuenta(7L);

        verify(tokenRepository).deleteAllByUsuarioId(7L);
        verify(respuestaRepository).deleteAllByIntentoUsuarioId(7L);
        verify(intentoRepository).deleteAllByUsuarioId(7L);
        assertNull(sharedQuiz.getUsuario());
        verify(cuestionarioRepository).save(sharedQuiz);
        verify(opcionRepository).deleteAllByCuestionarioId(11L);
        verify(preguntaRepository).deleteAllByCuestionarioId(11L);
        verify(cuestionarioRepository).delete(privateQuiz);
        verify(cuestionarioRepository, never()).delete(sharedQuiz);
        verify(usuarioRepository).delete(usuario);
    }

    private CambioPasswordForm passwordForm(String current, String next, String confirmation) {
        CambioPasswordForm form = new CambioPasswordForm();
        form.setPasswordActual(current);
        form.setPasswordNueva(next);
        form.setPasswordConfirmacion(confirmation);
        return form;
    }
}