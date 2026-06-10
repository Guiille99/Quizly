package com.quizly.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.quizly.app.repository.GeneroRepository;
import com.quizly.app.repository.PasswordResetTokenRepository;
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.dto.RegisterFormDto;
import com.quizly.app.exception.DuplicateEmailException;
import com.quizly.app.exception.DuplicateUsernameException;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Genero;
import com.quizly.app.model.entity.PasswordResetToken;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.Rol;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final GeneroRepository generoRepository; // Inyección del nuevo repositorio
    private final PasswordResetTokenRepository passwordResetTokenRepository; // Inyección del nuevo repositorio
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, GeneroRepository generoRepository, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.generoRepository = generoRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario register(RegisterFormDto registerDto) {
        String username = registerDto.getUsername().trim();
        String email = registerDto.getEmail().trim().toLowerCase();

        if (usuarioRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException();
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<Genero> generoOpt = generoRepository.findById(registerDto.getIdGenero()); // Usando el nuevo repositorio

        Usuario usuario = new Usuario();
        usuario.setNombre(registerDto.getNombre());
        usuario.setApellidos(registerDto.getApellidos());
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        usuario.setGenero(generoOpt.get());
        usuario.setRol(Rol.ESTUDIANTE);
        usuario.setCreatedAt(now);
        usuario.setUpdatedAt(now);

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public String createPasswordResetToken(Usuario usuario){
       List<PasswordResetToken> usuarioPrt = passwordResetTokenRepository.findAllByUserId(usuario.getId()); // Usando el nuevo repositorio
       
       // Si el usuario tiene tokens previos guardados en base de datos los eliminamos
       if (!CollectionUtils.isEmpty(usuarioPrt)) {
        List<Long> tokensIdList = usuarioPrt.stream()
            .map(PasswordResetToken::getId)
            .toList();
        
        passwordResetTokenRepository.deleteTokensById(tokensIdList); // Usando el nuevo repositorio
       }

        String token = UUID.randomUUID().toString();

        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setUsuario(usuario);
        prt.setFechaExpiracion(LocalDateTime.now().plusMinutes(50)); // Considera hacer este valor configurable

        passwordResetTokenRepository.save(prt); // Usando el nuevo repositorio
        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) throws QuizlyException{
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token) // Usando el nuevo repositorio
            .orElseThrow(() -> new QuizlyException("El token de recuperación es inválido."));

        if (prt.isExpired()) {
            passwordResetTokenRepository.delete(prt);
            throw new QuizlyException("El token ha expirado. Por favor, solicita uno nuevo.");
        }

        Usuario usuario = prt.getUsuario();
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario); // Ya usa el nuevo repositorio
        passwordResetTokenRepository.delete(prt); // Usando el nuevo repositorio
    }
}
