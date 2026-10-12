package com.quizly.app.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Cuestionario;

@Repository
public interface CuestionarioRepository extends JpaRepository<Cuestionario, Long>, CuestionarioRepositoryCustom {
    @Query("""
            SELECT c FROM Cuestionario c
            LEFT JOIN c.tema t
            WHERE c.version = (SELECT MAX(c2.version) FROM Cuestionario c2 WHERE c2.serie.id = c.serie.id)
            ORDER BY t.nombre ASC NULLS LAST, c.titulo ASC
            """)
    Page<Cuestionario> findAllByOrderByTemaNombreAsc(Pageable pageable);

    @Query("""
            SELECT c FROM Cuestionario c
            LEFT JOIN c.tema t
            WHERE (:search IS NULL OR :search = '' OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:dificultadId IS NULL OR c.dificultad.id = :dificultadId)
            AND c.version = (SELECT MAX(c2.version) FROM Cuestionario c2 WHERE c2.serie.id = c.serie.id)
            ORDER BY t.nombre ASC NULLS LAST, c.titulo ASC
            """)
    Page<Cuestionario> findAllByFilters(String search, Long dificultadId, Pageable pageable);

    @Query("""
            SELECT c FROM Cuestionario c
            WHERE c.usuario.id = :usuarioId
            AND c.version = (SELECT MAX(c2.version) FROM Cuestionario c2 WHERE c2.serie.id = c.serie.id)
            ORDER BY c.createdAt DESC
            """)
    Page<Cuestionario> findByUsuarioId(Long usuarioId, Pageable pageable);

    @Query("""
            SELECT c FROM Cuestionario c
            WHERE c.usuario.id = :usuarioId
            AND (:search IS NULL OR :search = '' OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:dificultadId IS NULL OR c.dificultad.id = :dificultadId)
            AND c.version = (SELECT MAX(c2.version) FROM Cuestionario c2 WHERE c2.serie.id = c.serie.id)
            ORDER BY c.createdAt DESC
            """)
    Page<Cuestionario> findByUsuarioIdAndFilters(Long usuarioId, String search, Long dificultadId, Pageable pageable);

    /**
     * Devuelve los cuestionarios recientes realizados por el usuario
     * cuyo estado del intento coincide con el pasado por parámetro
     * @param usuarioId
     * @param listIntentosCuestionarioEstado
     * @param pageable
     * @return Lista de cuestionarios paginada
     */

    @Query("""
            SELECT c FROM IntentoCuestionario ic
            JOIN ic.cuestionario c
            JOIN ic.estado e
            WHERE ic.usuario.id = :usuarioId AND e.codigo IN :listIntentosCuestionarioEstado
            GROUP BY c
            ORDER BY MAX(ic.fechaFin) DESC
            """)
    Page<Cuestionario> getCuestionariosRecientesByUsuarioYEstado(Long usuarioId, List<String> listIntentosCuestionarioEstado, Pageable pageable);

    @Query("""
            SELECT c FROM IntentoCuestionario ic
            JOIN ic.cuestionario c
            JOIN ic.estado e
            WHERE ic.usuario.id = :usuarioId AND e.codigo IN :listIntentosCuestionarioEstado
            AND (:search IS NULL OR :search = '' OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:dificultadId IS NULL OR c.dificultad.id = :dificultadId)
            GROUP BY c
            ORDER BY MAX(ic.fechaFin) DESC
            """)
    Page<Cuestionario> getCuestionariosRecientesByUsuarioYEstadoAndFilters(
            Long usuarioId,
            List<String> listIntentosCuestionarioEstado,
            String search,
            Long dificultadId,
            Pageable pageable);

    /**
     * Obtiene los cuestionarios de un tema paginados
     * @param temaId
     * @param pageable
     * @return Page<{@link Cuestionario}>
     */
    @Query("""
            SELECT c FROM Cuestionario c
            WHERE c.tema.id = :temaId
            AND c.version = (SELECT MAX(c2.version) FROM Cuestionario c2 WHERE c2.serie.id = c.serie.id)
            """)
    Page<Cuestionario> findByTemaId(Long temaId, Pageable pageable);

    @Query("""
            SELECT c FROM Cuestionario c
            WHERE LOWER(c.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
            AND c.tema.id = :temaId
            AND c.version = (SELECT MAX(c2.version) FROM Cuestionario c2 WHERE c2.serie.id = c.serie.id)
            """)
    Page<Cuestionario> findByTituloContainingIgnoreCaseAndTemaId(String titulo, Long temaId, Pageable pageable);

    @Query("""
            SELECT c FROM Cuestionario c
            WHERE c.tema.id = :temaId
            AND (:search IS NULL OR :search = '' OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:dificultadId IS NULL OR c.dificultad.id = :dificultadId)
            AND c.version = (SELECT MAX(c2.version) FROM Cuestionario c2 WHERE c2.serie.id = c.serie.id)
            ORDER BY c.titulo ASC
            """)
    Page<Cuestionario> findByTemaIdAndFilters(Long temaId, String search, Long dificultadId, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByDificultadCodigo(String codigoDificultad);

    /**
     * Obtiene los identificadores de las últimas versiones de los cuestionarios
     * cuyos identificadores se encuentran en la colección proporcionada.
     * 
     * @param ids Identificadores de los cuestionarios que se quieren comprobar
     * @return Conjunto de identificadores correspondientes a las últimas versiones
     * de los cuestionarios indicados
     */
    
    @Query("""
        SELECT c.id
        FROM Cuestionario c
        WHERE c.version = (
                SELECT MAX(c2.version)
                FROM Cuestionario c2
                WHERE c2.serie.id = c.serie.id
        )
        AND c.id IN :ids
        """)
    Set<Long> findLastVersionIds(Collection<Long> ids);

        @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cuestionario c WHERE c.serie.slug = :slug AND c.version > :version")
        boolean existsBySlugAndVersionGreaterThan(String slug, Integer version);

        boolean existsBySerie_Id(Long serieId);

        List<Cuestionario> findByUsuario_Id(Long usuarioId);
}