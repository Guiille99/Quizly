package com.quizly.app.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.quizly.app.model.entity.Usuario;
import com.quizly.app.service.AuthService;

/** Convierte el usuario OIDC de Google en un {@link QuizlyUserDetails} respaldado por un Usuario local. */
@Service
public class QuizlyOidcUserService extends OidcUserService {

    private final AuthService authService;

    public QuizlyOidcUserService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        // Enlazar por email solo es seguro si Google garantiza que el email es del usuario
        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified()) || oidcUser.getEmail() == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_verified"));
        }

        Usuario usuario = authService.findOrCreateGoogleUser(
                oidcUser.getSubject(), oidcUser.getEmail(), oidcUser.getGivenName(), oidcUser.getFamilyName());
        return new QuizlyUserDetails(usuario, oidcUser);
    }
}
