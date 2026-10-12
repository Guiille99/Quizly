package com.quizly.app.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.quizly.app.dto.form.CambioPasswordForm;
import com.quizly.app.dto.form.PerfilForm;
import com.quizly.app.dto.form.UsuarioForm;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Genero;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.CuestionarioSerieRepository;
import com.quizly.app.repository.GeneroRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.repository.OpcionRespuestaRepository;
import com.quizly.app.repository.PasswordResetTokenRepository;
import com.quizly.app.repository.PreguntaRepository;
import com.quizly.app.repository.RespuestaUsuarioRepository;
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
    private final CuestionarioRepository cuestionarioRepository;
    private final CuestionarioSerieRepository cuestionarioSerieRepository;
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;
    private final RespuestaUsuarioRepository respuestaUsuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OpcionRespuestaRepository opcionRespuestaRepository;
    private final PreguntaRepository preguntaRepository;

    public Usuario getUsuario(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el usuario"));
    }

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

    @Transactional
    public Usuario updatePerfil(Long id, PerfilForm form) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el usuario"));

        String username = form.getUsername().trim();
        String email = form.getEmail().trim();
        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }

        usuario.setNombre(form.getNombre().trim());
        usuario.setApellidos(form.getApellidos().trim());
        usuario.setEmail(email);
        usuario.setUsername(username);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario changePassword(Long id, CambioPasswordForm form) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el usuario"));

        if (!passwordEncoder.matches(form.getPasswordActual(), usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        if (!form.isPasswordMatch()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }

        usuario.setPassword(passwordEncoder.encode(form.getPasswordNueva()));
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void deleteCuenta(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el usuario"));

        passwordResetTokenRepository.deleteAllByUsuarioId(id);
        respuestaUsuarioRepository.deleteAllByIntentoUsuarioId(id);
        intentoCuestionarioRepository.deleteAllByUsuarioId(id);

        for (Cuestionario cuestionario : cuestionarioRepository.findByUsuario_Id(id)) {
            if (intentoCuestionarioRepository.existsByCuestionario_IdAndUsuario_IdNot(cuestionario.getId(), id)) {
                cuestionario.setUsuario(null);
                cuestionarioRepository.save(cuestionario);
                continue;
            }

            opcionRespuestaRepository.deleteAllByCuestionarioId(cuestionario.getId());
            preguntaRepository.deleteAllByCuestionarioId(cuestionario.getId());
            cuestionarioRepository.delete(cuestionario);
        }

        cuestionarioSerieRepository.deleteOrphanedSeries();

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
