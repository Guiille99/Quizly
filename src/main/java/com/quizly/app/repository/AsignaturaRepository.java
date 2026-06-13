package com.quizly.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Asignatura;

@Repository
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long>, AsignaturaRepositoryCustom {
    Page<Asignatura> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}