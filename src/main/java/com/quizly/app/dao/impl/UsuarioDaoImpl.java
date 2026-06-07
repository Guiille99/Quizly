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
        return entityManager.createQuery("SELECT u FROM Usuario u WHERE u.username = :username", Usuario.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return entityManager.createQuery("SELECT u FROM Usuario u WHERE u.email = :email", Usuario.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        return entityManager.createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.username = :username", Long.class)
                    .setParameter("username", username)
                    .getSingleResult() > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return entityManager.createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult() > 0;
    }
}
