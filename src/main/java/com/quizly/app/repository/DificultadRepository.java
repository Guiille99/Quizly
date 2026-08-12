package com.quizly.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Dificultad;

@Repository
public interface DificultadRepository extends JpaRepository<Dificultad, Long>, DificultadRepositoryCustom {
    List<Dificultad> findAllByOrderByNombreAsc();
}