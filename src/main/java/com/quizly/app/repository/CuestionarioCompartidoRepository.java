package com.quizly.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.CuestionarioCompartido;
import com.quizly.app.model.enums.EstadoComparticion;

@Repository
public interface CuestionarioCompartidoRepository extends JpaRepository<CuestionarioCompartido, Long> {

    @Query("SELECT c FROM CuestionarioCompartido c " +
           "JOIN FETCH c.emisor e " +
           "JOIN FETCH c.cuestionarioOrigen co " +
           "LEFT JOIN FETCH co.serie " +
           "WHERE c.receptor.id = :receptorId AND c.estado = :estado " +
           "ORDER BY c.fechaEnvio DESC")
    List<CuestionarioCompartido> findByReceptorIdAndEstadoOrderByFechaEnvioDesc(
            @Param("receptorId") Long receptorId,
            @Param("estado") EstadoComparticion estado);

    long countByReceptorIdAndEstado(Long receptorId, EstadoComparticion estado);

    boolean existsByEmisorIdAndReceptorIdAndCuestionarioOrigenIdAndEstado(
            Long emisorId, Long receptorId, Long cuestionarioOrigenId, EstadoComparticion estado);

    @Query("SELECT COUNT(c) > 0 FROM CuestionarioCompartido c " +
           "WHERE c.receptor.id = :receptorId " +
           "AND c.cuestionarioOrigen.id = :cuestionarioOrigenId " +
           "AND c.estado = :estado " +
           "AND c.cuestionarioClonado IS NOT NULL")
    boolean existsByReceptorIdAndCuestionarioOrigenIdAndEstadoAndCuestionarioClonadoIsNotNull(
            @Param("receptorId") Long receptorId,
            @Param("cuestionarioOrigenId") Long cuestionarioOrigenId,
            @Param("estado") EstadoComparticion estado);
}

