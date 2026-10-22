package com.quizly.app.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.CuestionarioCompartido;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.CuestionarioCompartidoService;
import com.quizly.app.web.notification.ToastService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final CuestionarioCompartidoService cuestionarioCompartidoService;
    private final ToastService toastService;

    @GetMapping("/dropdown")
    public String getDropdownContent(@AuthenticationPrincipal QuizlyUserDetails authUser, Model model) {
        List<CuestionarioCompartido> compartidos = cuestionarioCompartidoService.obtenerPendientesParaReceptor(authUser.getId());
        model.addAttribute("compartidosPendientes", compartidos);
        model.addAttribute("numNotificacionesPendientes", compartidos.size());
        return "fragments/notificaciones-dropdown :: dropdown-content";
    }

    @PostMapping("/compartidos/{id}/aceptar")
    public String aceptarCompartido(
            @PathVariable Long id,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            HttpServletResponse response,
            Model model) {

        try {
            Cuestionario clonado = cuestionarioCompartidoService.aceptarCompartido(id, authUser.getId());
            toastService.success(response, "Cuestionario '" + clonado.getTitulo() + "' añadido a tu colección.");
        } catch (QuizlyException e) {
            log.warn("Error al aceptar cuestionario compartido {}: {}", id, e.getMessage());
            toastService.error(response, e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al aceptar cuestionario compartido {}", id, e);
            toastService.error(response, "No se pudo aceptar el cuestionario.");
        }

        List<CuestionarioCompartido> compartidos = cuestionarioCompartidoService.obtenerPendientesParaReceptor(authUser.getId());
        model.addAttribute("compartidosPendientes", compartidos);
        model.addAttribute("numNotificacionesPendientes", compartidos.size());
        return "fragments/notificaciones-dropdown :: dropdown-content";
    }

    @PostMapping("/compartidos/{id}/rechazar")
    public String rechazarCompartido(
            @PathVariable Long id,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            HttpServletResponse response,
            Model model) {

        try {
            cuestionarioCompartidoService.rechazarCompartido(id, authUser.getId());
            toastService.info(response, "Cuestionario compartido rechazado.");
        } catch (QuizlyException e) {
            log.warn("Error al rechazar cuestionario compartido {}: {}", id, e.getMessage());
            toastService.error(response, e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al rechazar cuestionario compartido {}", id, e);
            toastService.error(response, "No se pudo rechazar la solicitud.");
        }

        List<CuestionarioCompartido> compartidos = cuestionarioCompartidoService.obtenerPendientesParaReceptor(authUser.getId());
        model.addAttribute("compartidosPendientes", compartidos);
        model.addAttribute("numNotificacionesPendientes", compartidos.size());
        return "fragments/notificaciones-dropdown :: dropdown-content";
    }
}

