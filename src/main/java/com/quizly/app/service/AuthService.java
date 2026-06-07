package com.quizly.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.quizly.app.dao.GeneroDao;
import com.quizly.app.dao.PasswordResetTokenDao;
import com.quizly.app.dao.UsuarioDao;
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

    private final UsuarioDao usuarioDao;
    private final GeneroDao generoDao;
    private final PasswordResetTokenDao passwordResetTokenDao;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioDao usuarioDao, PasswordEncoder passwordEncoder, GeneroDao generoDao, PasswordResetTokenDao passwordResetTokenDao) {
        this.usuarioDao = usuarioDao;
        this.generoDao = generoDao;
        this.passwordResetTokenDao = passwordResetTokenDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario register(RegisterFormDto registerDto) {
        String username = registerDto.getUsername().trim();
        String email = registerDto.getEmail().trim().toLowerCase();

        if (usuarioDao.existsByUsername(username)) {
            throw new DuplicateUsernameException();
        }
        if (usuarioDao.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<Genero> generoOpt = generoDao.findById(registerDto.getIdGenero());

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

        return usuarioDao.save(usuario);
    }

    @Transactional
    public String createPasswordResetToken(Usuario usuario){
       List<PasswordResetToken> usuarioPrt = passwordResetTokenDao.findAllByUserId(usuario.getId());
       
       // Si el usuario tiene tokens previos guardados en base de datos los eliminamos
       if (!CollectionUtils.isEmpty(usuarioPrt)) {
        List<Long> tokensIdList = usuarioPrt.stream()
            .map(PasswordResetToken::getId)
            .toList();

        passwordResetTokenDao.deleteTokensById(tokensIdList);
       }

        String token = UUID.randomUUID().toString();

        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setUsuario(usuario);
        prt.setFechaExpiracion(LocalDateTime.now().plusMinutes(50));

        passwordResetTokenDao.save(prt);
        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) throws QuizlyException{
        PasswordResetToken prt = passwordResetTokenDao.findByToken(token)
            .orElseThrow(() -> new QuizlyException("El token de recuperación es inválido."));

        if (prt.isExpired()) {
            passwordResetTokenDao.delete(prt);
            throw new QuizlyException("El token ha expirado. Por favor, solicita uno nuevo.");
        }

        Usuario usuario = prt.getUsuario();
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioDao.update(usuario);
        passwordResetTokenDao.delete(prt);
    }
}
