package com.quizly.app.dao.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.quizly.app.dao.UsuarioDao;
import com.quizly.app.model.entity.Usuario;

@Repository
public class UsuarioDaoImpl extends AbstractDaoImpl<Usuario, Long> implements UsuarioDao {

    public UsuarioDaoImpl() {
        super(Usuario.class);
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return findSingleResult(
                "SELECT u FROM Usuario u WHERE u.username = :username",
                "username",
                username);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return findSingleResult(
                "SELECT u FROM Usuario u WHERE u.email = :email",
                "email",
                email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return countByQuery(
                "SELECT COUNT(u) FROM Usuario u WHERE u.username = :username",
                "username",
                username) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return countByQuery(
                "SELECT COUNT(u) FROM Usuario u WHERE u.email = :email",
                "email",
                email) > 0;
    }

    private Optional<Usuario> findSingleResult(String jpql, String paramName, Object paramValue) {
        return entityManager.createQuery(jpql, Usuario.class)
                .setParameter(paramName, paramValue)
                .getResultStream()
                .findFirst();
    }

    private long countByQuery(String jpql, String paramName, Object paramValue) {
        return entityManager.createQuery(jpql, Long.class)
                .setParameter(paramName, paramValue)
                .getSingleResult();
    }
}
