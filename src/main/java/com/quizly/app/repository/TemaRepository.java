package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Tema;

@Repository
public interface TemaRepository extends JpaRepository<Tema, Long>, TemaRepositoryCustom {
}