package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.IntentoCuestionario;

@Repository
public interface IntentoCuestionarioRepository extends JpaRepository<IntentoCuestionario, Long>, IntentoCuestionarioRepositoryCustom {
    // Métodos CRUD, Paginación y Sorting heredados automáticamente
}