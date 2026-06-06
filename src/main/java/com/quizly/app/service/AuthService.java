package com.quizly.app.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quizly.app.dao.GeneroDao;
import com.quizly.app.dao.UsuarioDao;
import com.quizly.app.dto.RegisterFormDto;
import com.quizly.app.exception.DuplicateEmailException;
import com.quizly.app.exception.DuplicateUsernameException;
import com.quizly.app.model.entity.Genero;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.Rol;

@Service
public class AuthService {

    private final UsuarioDao usuarioDao;
    private final GeneroDao generoDao;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioDao usuarioDao, PasswordEncoder passwordEncoder, GeneroDao generoDao) {
        this.usuarioDao = usuarioDao;
        this.generoDao = generoDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario register(RegisterFormDto registerDto) {
        if (usuarioDao.existsByUsername(registerDto.getUsername())) {
            throw new DuplicateUsernameException();
        }
        if (usuarioDao.existsByEmail(registerDto.getEmail())) {
            throw new DuplicateEmailException();
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<Genero> generoOpt = generoDao.findById(registerDto.getIdGenero());

        Usuario usuario = new Usuario();
        usuario.setNombre(registerDto.getNombre());
        usuario.setApellidos(registerDto.getApellidos());
        usuario.setUsername(registerDto.getUsername().trim());
        usuario.setEmail(registerDto.getEmail().trim().toLowerCase());
        usuario.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        usuario.setGenero(generoOpt.get());
        usuario.setRol(Rol.ESTUDIANTE);
        usuario.setCreatedAt(now);
        usuario.setUpdatedAt(now);

        return usuarioDao.save(usuario);
    }
}
