package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Cuestionario;

@Repository
public interface CuestionarioRepository extends JpaRepository<Cuestionario, Long>, CuestionarioRepositoryCustom {
}