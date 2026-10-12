package com.quizly.app.repository;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Favorito;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO favoritos (usuario_id, serie_id, created_at)
            VALUES (:usuarioId, :serieId, CURRENT_TIMESTAMP)
            ON CONFLICT (usuario_id, serie_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("usuarioId") Long usuarioId, @Param("serieId") Long serieId);

        @Modifying
        @Query("DELETE FROM Favorito f WHERE f.usuario.id = :usuarioId AND f.serie.slug = :slug")
        int deleteByUsuarioAndSlug(@Param("usuarioId") Long usuarioId, @Param("slug") String slug);

        boolean existsByUsuario_IdAndSerie_Slug(Long usuarioId, String slug);

    @Query("SELECT f.serie.id FROM Favorito f WHERE f.usuario.id = :usuarioId AND f.serie.id IN :serieIds")
    Set<Long> findSerieIdsByUsuarioAndSerieIds(
            @Param("usuarioId") Long usuarioId,
            @Param("serieIds") Collection<Long> serieIds);

    @Query(value = """
            SELECT c FROM Favorito f
            JOIN Cuestionario c ON c.serie = f.serie
            WHERE f.usuario.id = :usuarioId
            AND (:search IS NULL OR :search = '' OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:dificultadId IS NULL OR c.dificultad.id = :dificultadId)
            AND c.version = (
                SELECT MAX(c2.version) FROM Cuestionario c2 WHERE c2.serie.id = f.serie.id
            )
            ORDER BY f.createdAt DESC, f.id DESC
            """, countQuery = """
            SELECT COUNT(f) FROM Favorito f
            JOIN Cuestionario c ON c.serie = f.serie
            WHERE f.usuario.id = :usuarioId
            AND (:search IS NULL OR :search = '' OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:dificultadId IS NULL OR c.dificultad.id = :dificultadId)
            AND c.version = (
                SELECT MAX(c2.version) FROM Cuestionario c2 WHERE c2.serie.id = f.serie.id
            )
            """)
    Page<Cuestionario> findFavoriteQuizzes(
            @Param("usuarioId") Long usuarioId,
            @Param("search") String search,
            @Param("dificultadId") Long dificultadId,
            Pageable pageable);
}