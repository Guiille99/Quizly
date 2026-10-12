package com.quizly.app.service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.FavoritoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoritoService {
    private final FavoritoRepository favoritoRepository;
    private final CuestionarioRepository cuestionarioRepository;

    @Transactional
    public boolean addFavorite(Long usuarioId, String slug) {
        Cuestionario cuestionario = cuestionarioRepository.findLastVersionBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el cuestionario."));
        return favoritoRepository.insertIfAbsent(usuarioId, cuestionario.getSerie().getId()) > 0;
    }

    @Transactional
    public void removeFavorite(Long usuarioId, String slug) {
        favoritoRepository.deleteByUsuarioAndSlug(usuarioId, slug);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long usuarioId, String slug) {
        return favoritoRepository.existsByUsuario_IdAndSerie_Slug(usuarioId, slug);
    }

    @Transactional(readOnly = true)
    public Set<Long> getFavoriteSerieIds(Long usuarioId, Collection<Cuestionario> cuestionarios) {
        Set<Long> serieIds = cuestionarios.stream()
                .map(cuestionario -> cuestionario.getSerie().getId())
                .collect(Collectors.toSet());
        if (serieIds.isEmpty()) {
            return Set.of();
        }
        return favoritoRepository.findSerieIdsByUsuarioAndSerieIds(usuarioId, serieIds);
    }

    @Transactional(readOnly = true)
    public Page<Cuestionario> getFavorites(Long usuarioId, String search, Long dificultadId, Pageable pageable) {
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;
        return favoritoRepository.findFavoriteQuizzes(usuarioId, cleanSearch, dificultadId, pageable);
    }
}