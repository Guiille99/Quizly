package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Pregunta;

@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Long>, PreguntaRepositoryCustom {
}