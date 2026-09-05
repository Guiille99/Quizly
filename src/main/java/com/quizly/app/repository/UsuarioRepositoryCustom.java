package com.quizly.app.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.quizly.app.dto.form.UsuarioFilterForm;
import com.quizly.app.model.entity.Usuario;

public interface UsuarioRepositoryCustom {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<Usuario> findAllWithFilters(UsuarioFilterForm filters, Pageable pageable);
}