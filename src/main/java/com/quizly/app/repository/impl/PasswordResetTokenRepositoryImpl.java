package com.quizly.app.repository.impl;

import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.PasswordResetToken;
import com.quizly.app.repository.PasswordResetTokenRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        TypedQuery<PasswordResetToken> query = entityManager.createQuery(
            "SELECT t FROM PasswordResetToken t WHERE t.token = :token", PasswordResetToken.class);
        query.setParameter("token", token);
        return query.getResultStream().findFirst();
    }

    @Override
    public List<PasswordResetToken> findAllByUserId(Long userId) {
        TypedQuery<PasswordResetToken> query = entityManager.createQuery(
            "SELECT t FROM PasswordResetToken t WHERE t.usuario.id = :userId", PasswordResetToken.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @Override
    public void deleteTokensById(List<Long> tokensIdList) {
        if (tokensIdList != null && !tokensIdList.isEmpty()) {
            entityManager.createQuery("DELETE FROM PasswordResetToken t WHERE t.id IN :ids")
                .setParameter("ids", tokensIdList)
                .executeUpdate();
        }
    }
}