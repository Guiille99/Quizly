package com.quizly.app.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.CuestionarioDao;
import com.quizly.app.model.entity.Cuestionario;

@Repository
public class CuestionarioDaoImpl extends AbstractDaoImpl<Cuestionario, Long> implements CuestionarioDao {

    public CuestionarioDaoImpl() {
        super(Cuestionario.class);
    }

    @Override
    public Optional<Cuestionario> findBySlug(String slug) {
        return entityManager.createQuery(
                        "SELECT c FROM Cuestionario c WHERE c.slug = :slug", Cuestionario.class)
                .setParameter("slug", slug)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Cuestionario> findByTemaId(Long temaId) {
        return entityManager.createQuery(
                        "SELECT c FROM Cuestionario c WHERE c.tema.id = :temaId", Cuestionario.class)
                .setParameter("temaId", temaId)
                .getResultList();
    }

    @Override
    public List<Cuestionario> findByTemaIdAndDificultadId(Long temaId, Long dificultadId) {
        return entityManager.createQuery(
                        """
                        SELECT c FROM Cuestionario c
                        WHERE c.tema.id = :temaId AND c.dificultad.id = :dificultadId
                        """,
                        Cuestionario.class)
                .setParameter("temaId", temaId)
                .setParameter("dificultadId", dificultadId)
                .getResultList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return entityManager.createQuery(
                        "SELECT COUNT(c) FROM Cuestionario c WHERE c.slug = :slug", Long.class)
                .setParameter("slug", slug)
                .getSingleResult() > 0;
    }
}
