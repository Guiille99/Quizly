package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Dificultad;

@Repository
public interface DificultadRepository extends JpaRepository<Dificultad, Long>, DificultadRepositoryCustom {
    // Métodos CRUD estándar heredados de JpaRepository
}