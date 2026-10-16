package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.CuestionarioSerie;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.CuestionarioSerieRepository;
import com.quizly.app.repository.FavoritoRepository;

import jakarta.persistence.EntityNotFoundException;

class FavoritoServiceTest {
    private FavoritoRepository favoritoRepository;
    private CuestionarioRepository cuestionarioRepository;
    private FavoritoService favoritoService;

    @BeforeEach
    void setUp() {
        favoritoRepository = org.mockito.Mockito.mock(FavoritoRepository.class);
        cuestionarioRepository = org.mockito.Mockito.mock(CuestionarioRepository.class);
        favoritoService = new FavoritoService(favoritoRepository, cuestionarioRepository);
    }

    @Test
    void addFavoriteUsesLogicalSeriesAndAuthenticatedUserScope() {
        CuestionarioSerie serie = new CuestionarioSerie();
        serie.setId(17L);
        Cuestionario quiz = new Cuestionario();
        quiz.setSerie(serie);
        when(cuestionarioRepository.findLastAccessibleVersionBySlug("algebra-basica", 42L)).thenReturn(Optional.of(quiz));
        when(favoritoRepository.insertIfAbsent(42L, 17L)).thenReturn(1);

        assertTrue(favoritoService.addFavorite(42L, "algebra-basica"));

        verify(cuestionarioRepository).findLastAccessibleVersionBySlug("algebra-basica", 42L);
        verify(favoritoRepository).insertIfAbsent(42L, 17L);
    }

    @Test
    void addFavoriteIsIdempotentWhenDatabaseReportsConflict() {
        CuestionarioSerie serie = new CuestionarioSerie();
        serie.setId(17L);
        Cuestionario quiz = new Cuestionario();
        quiz.setSerie(serie);
        when(cuestionarioRepository.findLastAccessibleVersionBySlug("algebra-basica", 42L)).thenReturn(Optional.of(quiz));
        when(favoritoRepository.insertIfAbsent(42L, 17L)).thenReturn(0);

        assertFalse(favoritoService.addFavorite(42L, "algebra-basica"));
    }

    @Test
    void addFavoriteRejectsMissingQuizBeforeWriting() {
        when(cuestionarioRepository.findLastAccessibleVersionBySlug("missing", 42L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> favoritoService.addFavorite(42L, "missing"));

        verify(favoritoRepository, never()).insertIfAbsent(42L, 17L);
    }

    @Test
    void removeFavoriteScopesDeletionToUserAndSeries() {
        favoritoService.removeFavorite(42L, "algebra-basica");

        verify(favoritoRepository).deleteByUsuarioAndSlug(42L, "algebra-basica");
    }

    @Test
    void isFavoriteChecksTheAuthenticatedUserAndLogicalSlug() {
        when(favoritoRepository.existsByUsuario_IdAndSerie_Slug(42L, "algebra-basica")).thenReturn(true);

        assertTrue(favoritoService.isFavorite(42L, "algebra-basica"));

        verify(favoritoRepository).existsByUsuario_IdAndSerie_Slug(42L, "algebra-basica");
    }
}