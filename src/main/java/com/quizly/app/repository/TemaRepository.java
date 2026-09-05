package com.quizly.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Tema;

@Repository
public interface TemaRepository extends JpaRepository<Tema, Long>, TemaRepositoryCustom {
    Page<Tema> findByNombreContainingIgnoreCaseAndAsignaturaId(String nombre, Long asignaturaId, Pageable pageable);
    Page<Tema> findByAsignaturaId(Long asignaturaId, Pageable pageable);
    Page<Tema> findAllByOrderByUpdatedAt(Pageable pageable);
    List<Tema> findAllByOrderByNombreAsc();


    /**
     * Comprueba si existe un tema con el mismo orden asignado
     * dentro de la misma asignatura, excluyendo un tema en específico
     * @param asignaturaId
     * @param orden
     * @param idTemaAExcluir
     * @return
     */
    boolean existsByAsignaturaIdAndOrdenAndIdNot(Long asignaturaId, Integer orden, Long idTemaAExcluir);
    
    /**
     * Comprueba si existe un tema con el mismo nombre
     * dentro de la misma asignatura, excluyendo un tema en específico
     * @param asignaturaId
     * @param nombre
     * @param idTemaAExcluir
     * @return
     */
    boolean existsByAsignaturaIdAndNombreIgnoreCaseAndIdNot(Long asignaturaId, String nombre, Long idTemaAExcluir);
}