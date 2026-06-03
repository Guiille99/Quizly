package com.quizly.app.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.quizly.app.model.entity.Usuario;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Usuario> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof QuizlyUserDetails userDetails) {
            return Optional.of(userDetails.getUsuario());
        }
        return Optional.empty();
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(Usuario::getId);
    }
}
