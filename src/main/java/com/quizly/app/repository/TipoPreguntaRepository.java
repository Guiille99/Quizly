package com.quizly.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.TipoPregunta;

@Repository
public interface TipoPreguntaRepository extends JpaRepository<TipoPregunta, Long>, TipoPreguntaRepositoryCustom {
    List<TipoPregunta> findAllByOrderByNombreAsc();
}