package com.quizly.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
            WHERE ic.usuario.id = :usuarioId AND e.codigo = :codigoEstadoIntento
            ORDER BY ic.fechaFin DESC
            """)
    Page<IntentoCuestionario> getIntentosCuestionariosByUsuarioYEstado(Long usuarioId, String codigoEstadoIntento, Pageable pageable);
}