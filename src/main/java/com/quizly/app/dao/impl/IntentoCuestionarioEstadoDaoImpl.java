package com.quizly.app.dao.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.IntentoCuestionarioEstadoDao;
import com.quizly.app.model.entity.IntentoCuestionarioEstado;

@Repository
public class IntentoCuestionarioEstadoDaoImpl extends AbstractDaoImpl<IntentoCuestionarioEstado, Long>
        implements IntentoCuestionarioEstadoDao {

    public IntentoCuestionarioEstadoDaoImpl() {
        super(IntentoCuestionarioEstado.class);
    }

    @Override
    public Optional<IntentoCuestionarioEstado> findByCodigo(String codigo) {
        return entityManager.createQuery(
                        "SELECT e FROM IntentoCuestionarioEstado e WHERE e.codigo = :codigo",
                        IntentoCuestionarioEstado.class)
                .setParameter("codigo", codigo)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return entityManager.createQuery(
                        "SELECT COUNT(e) FROM IntentoCuestionarioEstado e WHERE e.codigo = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult() > 0;
    }
}
