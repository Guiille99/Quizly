package com.quizly.app.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.repository.DificultadRepository;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.BreadcrumbService;
import com.quizly.app.service.CuestionarioService;
import com.quizly.app.service.FavoritoService;
import com.quizly.app.util.Constantes;
import com.quizly.app.web.model.BreadcrumbContext;
import com.quizly.app.web.notification.ToastService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/favoritos")
public class FavoritosController {
    private static final String FAVORITE_LIST_TARGET = "lista-cuestionarios";

    private final FavoritoService favoritoService;
    private final CuestionarioService cuestionarioService;
    private final DificultadRepository dificultadRepository;
    private final BreadcrumbContext breadcrumbContext;
    private final BreadcrumbService breadcrumbService;
    private final ToastService toastService;

    @GetMapping
    public String showFavorites(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            @PageableDefault(size = 5) Pageable pageable,
            Model model,
            HttpServletRequest request) {

        loadFavorites(authUser.getId(), search, dificultadId, pageable, model);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.cuestionariosFavoritos());
        return "cuestionarios/favoritos";
    }

    @GetMapping("/table-data")
    public String getFavoritesTableData(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            @PageableDefault(size = 5) Pageable pageable,
            Model model) {

        loadFavorites(authUser.getId(), search, dificultadId, pageable, model);
        return "cuestionarios/favoritos :: lista-cuestionarios_fragment";
    }

    @PutMapping("/{slug}")
    public String addFavorite(
            @PathVariable String slug,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Model model,
            HttpServletResponse response) {

        Cuestionario cuestionario = getLastVersion(slug);
        boolean added = favoritoService.addFavorite(authUser.getId(), slug);
        toastService.success(response, added ? "Cuestionario añadido a favoritos." : "El cuestionario ya estaba en favoritos.");
        return favoriteButton(cuestionario, true, model);
    }

    @DeleteMapping("/{slug}")
    public String removeFavorite(
            @PathVariable String slug,
            @RequestHeader(name = "HX-Target", required = false) String target,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            @PageableDefault(size = 5) Pageable pageable,
            Model model,
            HttpServletResponse response) {

        Cuestionario cuestionario = getLastVersion(slug);
        favoritoService.removeFavorite(authUser.getId(), slug);
        toastService.success(response, "Cuestionario eliminado de favoritos.");

        if (FAVORITE_LIST_TARGET.equals(target)) {
            loadFavorites(authUser.getId(), search, dificultadId, pageable, model);
            return "cuestionarios/favoritos :: lista-cuestionarios_fragment";
        }

        return favoriteButton(cuestionario, false, model);
    }

    private Cuestionario getLastVersion(String slug) {
        return cuestionarioService.getLastVersionQuizBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el cuestionario."));
    }

    private String favoriteButton(Cuestionario cuestionario, boolean isFavorite, Model model) {
        model.addAttribute("cuestionario", cuestionario);
        model.addAttribute("isFavorite", isFavorite);
        model.addAttribute("favoriteListPage", false);
        return "components/quiz/favorite-button :: response";
    }

    private void loadFavorites(Long usuarioId, String search, Long dificultadId, Pageable pageable, Model model) {
        Page<Cuestionario> favoritesPage = favoritoService.getFavorites(usuarioId, search, dificultadId, pageable);
        if (favoritesPage.isEmpty() && favoritesPage.getTotalPages() > 0
                && pageable.getPageNumber() >= favoritesPage.getTotalPages()) {
            Pageable lastPage = PageRequest.of(favoritesPage.getTotalPages() - 1, pageable.getPageSize());
            favoritesPage = favoritoService.getFavorites(usuarioId, search, dificultadId, lastPage);
        }

        model.addAttribute("favoritesPage", favoritesPage);
        model.addAttribute("favoriteListPage", true);
        model.addAttribute("dificultades", dificultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("search", search);
        model.addAttribute("dificultadId", dificultadId);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> handleMissingQuiz(HttpServletResponse response) {
        toastService.error(response, "No se ha podido actualizar el favorito.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}