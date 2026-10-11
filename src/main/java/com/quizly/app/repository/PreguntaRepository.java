package com.quizly.app.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Pregunta;

@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Long>, PreguntaRepositoryCustom {
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("DELETE FROM Pregunta pregunta WHERE pregunta.cuestionario.id = :cuestionarioId")
    int deleteAllByCuestionarioId(@Param("cuestionarioId") Long cuestionarioId);
}