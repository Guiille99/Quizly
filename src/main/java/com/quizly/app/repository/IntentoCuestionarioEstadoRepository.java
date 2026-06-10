package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.IntentoCuestionarioEstado;

@Repository
public interface IntentoCuestionarioEstadoRepository extends JpaRepository<IntentoCuestionarioEstado, Long>, IntentoCuestionarioEstadoRepositoryCustom {
    // Métodos CRUD estándar heredados de JpaRepository
}