package com.quizly.app.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.IntentoCuestionario;

@Repository
public interface IntentoCuestionarioRepository extends JpaRepository<IntentoCuestionario, Long>, IntentoCuestionarioRepositoryCustom {

    /**
     * Obtiene la lista paginada de los intentos de cuestionarios del usuario
     * cuyo estado coincida con el pasado por parámetro
     * @param usuarioId
     * @param codigoEstadoIntento
     * @param pageable
     * @return Lista paginada de intentos de cuestionarios
     */
    @Query("""
            SELECT ic FROM IntentoCuestionario ic
            JOIN ic.estado e
            JOIN ic.cuestionario c
            WHERE ic.usuario.id = :usuarioId AND e.codigo = :codigoEstadoIntento
            AND (c.usuario IS NULL OR c.usuario.id = :usuarioId)
            ORDER BY ic.fechaFin DESC
            """)
    Page<IntentoCuestionario> getIntentosCuestionariosByUsuarioYEstado(Long usuarioId, String codigoEstadoIntento, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    boolean existsByCuestionarioId(long cuestionarioId);

    boolean existsByCuestionario_IdAndUsuario_IdNot(Long cuestionarioId, Long usuarioId);

    @Modifying
    @Query("DELETE FROM IntentoCuestionario intento WHERE intento.usuario.id = :usuarioId")
    int deleteAllByUsuarioId(@Param("usuarioId") Long usuarioId);
}