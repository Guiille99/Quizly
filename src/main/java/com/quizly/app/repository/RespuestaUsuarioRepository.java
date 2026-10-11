package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.RespuestaUsuario;

@Repository
public interface RespuestaUsuarioRepository extends JpaRepository<RespuestaUsuario, Long>, RespuestaUsuarioRepositoryCustom {
	@Modifying
	@Query("DELETE FROM RespuestaUsuario respuesta WHERE respuesta.intento.usuario.id = :usuarioId")
	int deleteAllByIntentoUsuarioId(@Param("usuarioId") Long usuarioId);
}