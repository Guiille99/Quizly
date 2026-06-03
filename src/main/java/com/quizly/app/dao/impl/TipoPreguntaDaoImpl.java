package com.quizly.app.dao.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.TipoPreguntaDao;
import com.quizly.app.model.entity.TipoPregunta;

@Repository
public class TipoPreguntaDaoImpl extends AbstractDaoImpl<TipoPregunta, Long> implements TipoPreguntaDao {

    public TipoPreguntaDaoImpl() {
        super(TipoPregunta.class);
    }

    @Override
    public Optional<TipoPregunta> findByCodigo(String codigo) {
        return entityManager.createQuery(
                        "SELECT t FROM TipoPregunta t WHERE t.codigo = :codigo", TipoPregunta.class)
                .setParameter("codigo", codigo)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return entityManager.createQuery(
                        "SELECT COUNT(t) FROM TipoPregunta t WHERE t.codigo = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult() > 0;
    }
}
