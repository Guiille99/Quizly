package com.quizly.app.repository.impl;

import java.util.List;
import java.util.Optional;

import com.quizly.app.dto.TemaEstadisticaDto;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.repository.TemaRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class TemaRepositoryImpl implements TemaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Tema> findBySlug(String slug) {
        TypedQuery<Tema> query = entityManager.createQuery(
            "SELECT t FROM Tema t WHERE t.slug = :slug", Tema.class);
        query.setParameter("slug", slug);
        return query.getResultStream().findFirst();
    }

    @Override
    public List<Tema> findByAsignaturaId(Long asignaturaId) {
        TypedQuery<Tema> query = entityManager.createQuery(
            "SELECT t FROM Tema t WHERE t.asignatura.id = :asignaturaId ORDER BY t.orden ASC", 
            Tema.class);
        query.setParameter("asignaturaId", asignaturaId);
        return query.getResultList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(t) FROM Tema t WHERE t.slug = :slug", Long.class)
            .setParameter("slug", slug)
            .getSingleResult();
        
        return count > 0;
    }

    @Override
    public List<TemaEstadisticaDto> getEstadisticasTemasPorUsuario(Long usuarioId) {
       return entityManager.createQuery("""
                SELECT new com.quizly.app.dto.TemaEstadisticaDto(
                    t.id,
                    t.nombre, 
                    AVG(CAST(ic.aciertos AS double) * 100 / NULLIF(ic.aciertos + ic.fallos, 0))
                )
                FROM IntentoCuestionario ic
                JOIN ic.cuestionario c
                JOIN  c.tema t
                WHERE ic.usuario.id = :usuarioId
                GROUP BY t
               """, TemaEstadisticaDto.class)
               .setParameter("usuarioId", usuarioId)
               .getResultList();
    }
}