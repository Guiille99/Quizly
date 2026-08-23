package com.quizly.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Tema;

@Repository
public interface TemaRepository extends JpaRepository<Tema, Long>, TemaRepositoryCustom {
    Page<Tema> findByNombreContainingIgnoreCaseAndAsignaturaId(String nombre, Long asignaturaId, Pageable pageable);
    Page<Tema> findByAsignaturaId(Long asignaturaId, Pageable pageable);
    Page<Tema> findAllByOrderByUpdatedAt(Pageable pageable);
    List<Tema> findAllByOrderByNombreAsc();
    
}