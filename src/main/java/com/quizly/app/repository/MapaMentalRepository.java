package com.quizly.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quizly.app.model.entity.MapaMental;

public interface MapaMentalRepository extends JpaRepository<MapaMental, Long> {

    /** Con tema y asignatura ya cargados: la lista los pinta y así no hay una consulta por mapa. */
    @Query("""
            select m from MapaMental m
            left join fetch m.tema t
            left join fetch t.asignatura
            where m.usuario.id = :usuarioId
            order by m.titulo
            """)
    List<MapaMental> findConTemaByUsuarioId(@Param("usuarioId") Long usuarioId);

    Optional<MapaMental> findByIdAndUsuarioId(Long id, Long usuarioId);
}
