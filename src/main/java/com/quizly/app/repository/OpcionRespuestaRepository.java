package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.OpcionRespuesta;

@Repository
public interface OpcionRespuestaRepository extends JpaRepository<OpcionRespuesta, Long>, OpcionRespuestaRepositoryCustom {
	@Modifying
	@Query("DELETE FROM OpcionRespuesta opcion WHERE opcion.pregunta.cuestionario.id = :cuestionarioId")
	int deleteAllByCuestionarioId(@Param("cuestionarioId") Long cuestionarioId);
}