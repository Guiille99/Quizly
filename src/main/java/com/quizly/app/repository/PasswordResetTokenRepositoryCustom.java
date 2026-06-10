package com.quizly.app.repository;

import java.util.List;
import java.util.Optional;
import com.quizly.app.model.entity.PasswordResetToken;

public interface PasswordResetTokenRepositoryCustom {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findAllByUserId(Long userId);

    void deleteTokensById(List<Long> tokensIdList);
}