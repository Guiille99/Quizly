package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Asignatura;

@Repository
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long>, AsignaturaRepositoryCustom {
    // Aquí Spring Data JPA inyectará automáticamente los métodos CRUD estándar
}