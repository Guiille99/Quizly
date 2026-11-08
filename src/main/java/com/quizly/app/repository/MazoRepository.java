package com.quizly.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quizly.app.model.entity.Mazo;

public interface MazoRepository extends JpaRepository<Mazo, Long> {

    List<Mazo> findByUsuarioIdOrderByTitulo(Long usuarioId);

    Optional<Mazo> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Mazo> findByUsuarioIdAndDeFallosTrue(Long usuarioId);

    Optional<Mazo> findByUsuarioIdAndSerieId(Long usuarioId, Long serieId);

    // Get-or-create atómico: el índice único parcial resuelve la carrera sin abortar la transacción
    @Modifying
    @Query(value = """
            INSERT INTO mazos (titulo, usuario_id, de_fallos, created_at, updated_at)
            VALUES (:titulo, :usuarioId, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (usuario_id) WHERE de_fallos DO NOTHING
            """, nativeQuery = true)
    int insertMazoFallosIfAbsent(@Param("usuarioId") Long usuarioId, @Param("titulo") String titulo);

    @Modifying
    @Query(value = """
            INSERT INTO mazos (titulo, usuario_id, serie_id, de_fallos, created_at, updated_at)
            VALUES (:titulo, :usuarioId, :serieId, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (usuario_id, serie_id) WHERE serie_id IS NOT NULL DO NOTHING
            """, nativeQuery = true)
    int insertMazoSerieIfAbsent(@Param("usuarioId") Long usuarioId, @Param("serieId") Long serieId,
            @Param("titulo") String titulo);
}
