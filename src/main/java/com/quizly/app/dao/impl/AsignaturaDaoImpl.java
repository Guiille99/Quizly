package com.quizly.app.dao.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.AsignaturaDao;
import com.quizly.app.model.entity.Asignatura;

@Repository
public class AsignaturaDaoImpl extends AbstractDaoImpl<Asignatura, Long> implements AsignaturaDao {

    public AsignaturaDaoImpl() {
        super(Asignatura.class);
    }

    @Override
    public Optional<Asignatura> findBySlug(String slug) {
        return entityManager.createQuery(
                        "SELECT a FROM Asignatura a WHERE a.slug = :slug", Asignatura.class)
                .setParameter("slug", slug)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return entityManager.createQuery(
                        "SELECT COUNT(a) FROM Asignatura a WHERE a.slug = :slug", Long.class)
                .setParameter("slug", slug)
                .getSingleResult() > 0;
    }
}
