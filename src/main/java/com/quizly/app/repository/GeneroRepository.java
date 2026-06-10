package com.quizly.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quizly.app.model.entity.Genero;

@Repository
public interface GeneroRepository extends JpaRepository<Genero, Long>, GeneroRepositoryCustom {
    // JpaRepository ya nos proporciona findAll(), findById(), etc.
}