package com.quizly.app.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, UsuarioRepositoryCustom {
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByUsernameIgnoreCase(String username);
    
    boolean existsByEmailIgnoreCase(String email);
    
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
    
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}