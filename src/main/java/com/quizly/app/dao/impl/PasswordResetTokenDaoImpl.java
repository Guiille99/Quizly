package com.quizly.app.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import com.quizly.app.dao.PasswordResetTokenDao;
import com.quizly.app.model.entity.PasswordResetToken;

@Repository
public class PasswordResetTokenDaoImpl extends AbstractDaoImpl<PasswordResetToken, Long> implements PasswordResetTokenDao {

    protected PasswordResetTokenDaoImpl() {
        super(PasswordResetToken.class);
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return entityManager.createQuery("SELECT t FROM PasswordResetToken t WHERE t.token = :token",
            PasswordResetToken.class)
            .setParameter("token", token)
            .getResultStream()
            .findFirst();
    }

    @Override
    public List<PasswordResetToken> findAllByUserId(Long userId) {
        return entityManager.createQuery("SELECT t FROM PasswordResetToken t WHERE t.usuario.id = :userId", 
        PasswordResetToken.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    @Override
    public void deleteTokensById(List<Long> tokensIdList) {
        if (CollectionUtils.isEmpty(tokensIdList)) {
            return;
        }
        entityManager.createQuery("DELETE FROM PasswordResetToken t WHERE t.id IN :tokenIds")
            .setParameter("tokenIds", tokensIdList)
            .executeUpdate();
    }

}
