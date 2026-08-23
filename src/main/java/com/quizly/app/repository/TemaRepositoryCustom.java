package com.quizly.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.quizly.app.dto.TemaEstadisticaDto;
import com.quizly.app.model.entity.Tema;

public interface TemaRepositoryCustom {
    
    Optional<Tema> findBySlug(String slug);

    List<Tema> findByAsignaturaId(Long asignaturaId);

    boolean existsBySlug(String slug);

    /**
     * Obtiene la lista de estadística de los temas
     * asociados a un usuario
     * @param usuarioId
     * @return Lista de {@link TemaEstadisticaDto}
     */
    List<TemaEstadisticaDto> getEstadisticasTemasPorUsuario(Long usuarioId);

    Page<Tema> findAllWithFilters(Pageable pageable);
}