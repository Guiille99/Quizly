package com.quizly.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Cuestionario;

@Repository
public interface CuestionarioRepository extends JpaRepository<Cuestionario, Long>, CuestionarioRepositoryCustom {
    Page<Cuestionario> findAllByOrderByTemaNombreAsc(Pageable pageable);

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
}