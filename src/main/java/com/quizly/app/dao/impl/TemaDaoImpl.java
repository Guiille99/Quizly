package com.quizly.app.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.TemaDao;
import com.quizly.app.model.entity.Tema;

@Repository
public class TemaDaoImpl extends AbstractDaoImpl<Tema, Long> implements TemaDao {

    public TemaDaoImpl() {
        super(Tema.class);
    }

    @Override
    public Optional<Tema> findBySlug(String slug) {
        return entityManager.createQuery(
                        "SELECT t FROM Tema t WHERE t.slug = :slug", Tema.class)
                .setParameter("slug", slug)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Tema> findByAsignaturaId(Long asignaturaId) {
        return entityManager.createQuery(
                        "SELECT t FROM Tema t WHERE t.asignatura.id = :asignaturaId ORDER BY t.orden",
                        Tema.class)
                .setParameter("asignaturaId", asignaturaId)
                .getResultList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return entityManager.createQuery(
                        "SELECT COUNT(t) FROM Tema t WHERE t.slug = :slug", Long.class)
                .setParameter("slug", slug)
                .getSingleResult() > 0;
    }
}
