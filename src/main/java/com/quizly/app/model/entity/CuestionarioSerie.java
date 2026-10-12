package com.quizly.app.model.entity;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@BatchSize(size = 32)
@Table(name = "cuestionario_series", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cuestionario_series_slug", columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
public class CuestionarioSerie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;
}