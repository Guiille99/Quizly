package com.quizly.app.dao;

import java.util.Optional;

import com.quizly.app.model.entity.Usuario;

public interface UsuarioDao extends BaseDao<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
