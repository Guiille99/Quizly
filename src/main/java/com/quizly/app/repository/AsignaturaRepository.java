package com.quizly.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Asignatura;

@Repository
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long>, AsignaturaRepositoryCustom {
    Page<Asignatura> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    List<Asignatura> findAllByOrderByNombreAsc();

    /**
     * Consulta si existen asignaturas con el nombre
     * pasado por parámetro, excluyendo una asignatura en específico
     * @param nombre
     * @param id
     * @return
     */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
    
    boolean existsBySlug(String slug);
}