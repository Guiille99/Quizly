package com.quizly.app.repository;

import java.util.Optional;
import com.quizly.app.model.entity.Usuario;

public interface UsuarioRepositoryCustom {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}