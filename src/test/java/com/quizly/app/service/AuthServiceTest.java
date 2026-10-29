package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quizly.app.exception.DuplicateEmailException;
import com.quizly.app.model.entity.Genero;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.repository.GeneroRepository;
import com.quizly.app.repository.PasswordResetTokenRepository;
import com.quizly.app.repository.UsuarioRepository;

class AuthServiceTest {
    private UsuarioRepository usuarioRepository;
    private GeneroRepository generoRepository;
    private PasswordResetTokenRepository tokenRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        generoRepository = mock(GeneroRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        authService = new AuthService(usuarioRepository, mock(PasswordEncoder.class), generoRepository, tokenRepository);
        when(usuarioRepository.findByGoogleSub(any())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuario(String googleSub) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("ana");
        usuario.setEmail("ana@uni.es");
        usuario.setPassword("hash");
        usuario.setGoogleSub(googleSub);
        return usuario;
    }

    @Test
    void findOrCreateGoogleUser_coincideSub_devuelveUsuario() {
        Usuario existente = usuario("sub-1");
        when(usuarioRepository.findByGoogleSub("sub-1")).thenReturn(Optional.of(existente));

        assertSame(existente, authService.findOrCreateGoogleUser("sub-1", "ana@uni.es", "Ana", "Pérez"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void findOrCreateGoogleUser_emailSinSub_vinculaCuenta() {
        Usuario existente = usuario(null);
        when(usuarioRepository.findByEmail("ana@uni.es")).thenReturn(Optional.of(existente));

        Usuario result = authService.findOrCreateGoogleUser("sub-1", " Ana@Uni.es ", "Ana", "Pérez");

        assertEquals("sub-1", result.getGoogleSub());
        assertNull(result.getPassword());
        verify(tokenRepository).deleteAllByUsuarioId(7L);
        verify(usuarioRepository).save(existente);
    }

    @Test
    void findOrCreateGoogleUser_emailConOtroSub_rechaza() {
        Usuario existente = usuario("sub-original");
        when(usuarioRepository.findByEmail("ana@uni.es")).thenReturn(Optional.of(existente));

        assertThrows(DuplicateEmailException.class,
                () -> authService.findOrCreateGoogleUser("sub-intruso", "ana@uni.es", "Ana", "Pérez"));
        assertEquals("sub-original", existente.getGoogleSub());
        assertEquals("hash", existente.getPassword());
        verify(usuarioRepository, never()).save(any());
        verify(tokenRepository, never()).deleteAllByUsuarioId(anyLong());
    }

    @Test
    void findOrCreateGoogleUser_sinCoincidencias_creaUsuario() {
        Genero pnd = new Genero();
        when(generoRepository.findByCodigo("PND")).thenReturn(Optional.of(pnd));

        Usuario result = authService.findOrCreateGoogleUser("sub-nuevo", "Nuevo@Uni.es", "Nuevo", "Usuario");

        assertEquals("sub-nuevo", result.getGoogleSub());
        assertEquals("nuevo@uni.es", result.getEmail());
        assertEquals("nuevo", result.getUsername());
        assertSame(pnd, result.getGenero());
        verify(usuarioRepository).save(result);
    }
}
