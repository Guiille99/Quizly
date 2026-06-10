package com.quizly.app.repository.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Usuario;
import com.quizly.app.repository.UsuarioRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class UsuarioRepositoryImpl implements UsuarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

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
