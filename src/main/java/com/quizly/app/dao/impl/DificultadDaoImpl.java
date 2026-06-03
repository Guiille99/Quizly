package com.quizly.app.dao.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.DificultadDao;
import com.quizly.app.model.entity.Dificultad;

@Repository
public class DificultadDaoImpl extends AbstractDaoImpl<Dificultad, Long> implements DificultadDao {

    public DificultadDaoImpl() {
        super(Dificultad.class);
    }

    @Override
    public Optional<Dificultad> findByCodigo(String codigo) {
        return entityManager.createQuery(
                        "SELECT d FROM Dificultad d WHERE d.codigo = :codigo", Dificultad.class)
                .setParameter("codigo", codigo)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return entityManager.createQuery(
                        "SELECT COUNT(d) FROM Dificultad d WHERE d.codigo = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult() > 0;
    }
}
