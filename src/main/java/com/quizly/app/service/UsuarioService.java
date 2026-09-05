package com.quizly.app.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.quizly.app.dto.form.UsuarioForm;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Genero;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.repository.GeneroRepository;
import com.quizly.app.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final GeneroRepository generoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional 
    public Usuario createUsuario(UsuarioForm form) {
        validateUsuario(form);
        
        if (!StringUtils.hasText(form.getPassword())) {
            throw new IllegalArgumentException("La contraseña es obligatoria para nuevos usuarios.");
        }

        Usuario usuario = new Usuario();
        updateUsuarioFields(usuario, form);
        
        usuario.setPassword(passwordEncoder.encode(form.getPassword()));

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario updateUsuario(Long id, UsuarioForm form) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el usuario a actualizar"));

        validateUsuario(form);
        updateUsuarioFields(usuario, form);

        if (StringUtils.hasText(form.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void deleteUsuario(Long id) throws QuizlyException {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el usuario"));

        if (!CollectionUtils.isEmpty(usuario.getIntentos())) {
            throw new QuizlyException("No se puede eliminar el usuario porque tiene intentos de cuestionarios asociados.");
        }

        usuarioRepository.delete(usuario);
    }

    private void updateUsuarioFields(Usuario usuario, UsuarioForm form) {
        usuario.setNombre(form.getNombre());
        usuario.setApellidos(form.getApellidos());
        usuario.setUsername(form.getUsername());
        usuario.setEmail(form.getEmail());
        usuario.setRol(form.getRol());

        if (form.getGeneroId() != null) {
            Genero genero = generoRepository.findById(form.getGeneroId())
                .orElseThrow(() -> new IllegalArgumentException("Género no válido"));
            usuario.setGenero(genero);
        } else {
            usuario.setGenero(null);
        }
    }

    private void validateUsuario(UsuarioForm form) {
        Long id = form.getId() != null ? form.getId() : -1L;

        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(form.getUsername(), id)) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(form.getEmail(), id)) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }
    }
}
