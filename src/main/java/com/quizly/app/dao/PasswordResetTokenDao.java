package com.quizly.app.dao;

import java.util.List;
import java.util.Optional;

import com.quizly.app.model.entity.PasswordResetToken;

public interface PasswordResetTokenDao extends BaseDao<PasswordResetToken, Long> {

    /**
     * Obtiene el PasswordResetToken por el token pasado por parámetro
     * @param token
     * @return
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Obtiene todos los PasswordResetToken del usuario pasado por parámetro
     * @param userId
     * @return
     */
    List<PasswordResetToken> findAllByUserId(Long userId);

    /**
     * Elimina todos los PasswordResetToken cuyo ID estén en la lista
     * recibida por parámetro
     * @param tokensIdList
     */
    void deleteTokensById(List<Long> tokensIdList);
}
