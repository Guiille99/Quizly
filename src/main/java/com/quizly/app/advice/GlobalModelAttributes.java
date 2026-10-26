package com.quizly.app.advice;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.quizly.app.service.CuestionarioCompartidoService;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.util.Constantes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final CuestionarioCompartidoService cuestionarioCompartidoService;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @ModelAttribute("currentUser")
    public Usuario currentUser(@AuthenticationPrincipal QuizlyUserDetails user){
        return user != null ? user.getUsuario() : null;
    }

    @ModelAttribute(Constantes.ATTR_CURRENT_URI)
    public String currentUri(HttpServletRequest request){
        return request.getRequestURI();
    }

    @ModelAttribute("numNotificacionesPendientes")
    public long numNotificacionesPendientes(@AuthenticationPrincipal QuizlyUserDetails user) {
        if (user == null || user.getId() == null) {
            return 0;
        }
        return cuestionarioCompartidoService.contarPendientes(user.getId());
    }

    // BuildProperties solo existe si Maven generó build-info.properties (no al lanzar desde el IDE ni en tests)
    @ModelAttribute("appVersion")
    public String appVersion() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        return buildProperties != null ? buildProperties.getVersion() : null;
    }
}
