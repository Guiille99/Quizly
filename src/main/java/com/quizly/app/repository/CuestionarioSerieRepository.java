package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.quizly.app.model.entity.CuestionarioSerie;

import java.util.Optional;

public interface CuestionarioSerieRepository extends JpaRepository<CuestionarioSerie, Long> {
    boolean existsBySlug(String slug);

    Optional<CuestionarioSerie> findBySlug(String slug);

    @Modifying
    @Query("DELETE FROM CuestionarioSerie s WHERE NOT EXISTS (SELECT c.id FROM Cuestionario c WHERE c.serie = s)")
    int deleteOrphanedSeries();
}