package com.quizly.app.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.quizly.app.dao.IntentoCuestionarioDao;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.util.Constantes;
import com.quizly.app.web.notification.ToastService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class HomeController {
    // private final ToastService toastService;
    private final IntentoCuestionarioDao intentoCuestionarioDao;

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal QuizlyUserDetails userDetails, 
            HttpServletRequest request) {
        if (userDetails != null) {
            List<IntentoCuestionario> intentosCuestionarioUsuario = intentoCuestionarioDao.findByUsuarioId(userDetails.getId());

            List<IntentoCuestionario> cuestionariosCompletados = intentosCuestionarioUsuario.stream()
                .filter(i -> IntentoCuestionarioEstadoEnum.FINISHED.toString().equals(i.getEstado().getCodigo()))
                .toList();

            double puntuacionMedia = cuestionariosCompletados.stream()
                .mapToDouble(IntentoCuestionario::getPorcentajeAciertos)
                .average()
                .orElse(0.0);

            long nAsignaturasActivas = intentosCuestionarioUsuario.stream()
                .map(i -> i.getCuestionario().getTema().getAsignatura().getId())
                .distinct()
                .count();

            long totalSegundos = intentosCuestionarioUsuario.stream()
                .mapToLong(i -> i.getTiempoEmpleado() != null ? i.getTiempoEmpleado() : 0L)
                .sum();
            long minutosEstudio = totalSegundos / 60;

            model.addAttribute("nCuestionariosCompletados", cuestionariosCompletados.size());
            model.addAttribute("puntuacionMedia", Math.round(puntuacionMedia));
            model.addAttribute("nAsignaturasActivas", nAsignaturasActivas);
            model.addAttribute("minutosEstudio", minutosEstudio);
        }
        
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        
        return "index";
    }

   
    
}
