package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>, PasswordResetTokenRepositoryCustom {
	@Modifying
	@Query("DELETE FROM PasswordResetToken token WHERE token.usuario.id = :usuarioId")
	int deleteAllByUsuarioId(@Param("usuarioId") Long usuarioId);
}