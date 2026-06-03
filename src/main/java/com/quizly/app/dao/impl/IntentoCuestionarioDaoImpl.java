package com.quizly.app.dao.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.IntentoCuestionarioDao;
import com.quizly.app.model.entity.IntentoCuestionario;

@Repository
public class IntentoCuestionarioDaoImpl extends AbstractDaoImpl<IntentoCuestionario, Long>
        implements IntentoCuestionarioDao {

    public IntentoCuestionarioDaoImpl() {
        super(IntentoCuestionario.class);
    }

    @Override
    public List<IntentoCuestionario> findByUsuarioId(Long usuarioId) {
        return entityManager.createQuery(
                        "SELECT i FROM IntentoCuestionario i WHERE i.usuario.id = :usuarioId",
                        IntentoCuestionario.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }

    @Override
    public List<IntentoCuestionario> findByCuestionarioId(Long cuestionarioId) {
        return entityManager.createQuery(
                        "SELECT i FROM IntentoCuestionario i WHERE i.cuestionario.id = :cuestionarioId",
                        IntentoCuestionario.class)
                .setParameter("cuestionarioId", cuestionarioId)
                .getResultList();
    }

    @Override
    public List<IntentoCuestionario> findByUsuarioIdAndCuestionarioId(Long usuarioId, Long cuestionarioId) {
        return entityManager.createQuery(
                        """
                        SELECT i FROM IntentoCuestionario i
                        WHERE i.usuario.id = :usuarioId AND i.cuestionario.id = :cuestionarioId
                        """,
                        IntentoCuestionario.class)
                .setParameter("usuarioId", usuarioId)
                .setParameter("cuestionarioId", cuestionarioId)
                .getResultList();
    }
}
