package com.quizly.app.controller;

import com.quizly.app.service.TemaService;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.ActividadService;
import com.quizly.app.util.Constantes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class HomeController {
    private final TemaService temaService;
    // private final ToastService toastService;
    private final ActividadService actividadService;
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal QuizlyUserDetails authUser, 
            HttpServletRequest request) {
        if (authUser != null) {
            List<IntentoCuestionario> intentosCuestionarioUsuario = intentoCuestionarioRepository.findByUsuarioId(authUser.getId());

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
            model.addAttribute("actividades", actividadService.getActividadesRecientes(authUser.getUsuario(), 5));
            model.addAttribute("temasMayorRendimiento", temaService.getTemasMayorRendimientoUsuario(authUser.getId()));
            model.addAttribute("temasPeorRendimiento", temaService.getTemasPeorRendimientoUsuario(authUser.getId()));
        }
        
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        
        return "index";
    }

   
    
}
