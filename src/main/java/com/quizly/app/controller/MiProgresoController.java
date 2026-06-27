package com.quizly.app.controller;

import com.quizly.app.service.IntentoCuestionarioService;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.util.Constantes;
import com.quizly.app.util.Utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestParam;



@Controller
@RequiredArgsConstructor
public class MiProgresoController {
    private final IntentoCuestionarioService intentoCuestionarioService;
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;


    @GetMapping("/miProgreso")
    public String showMyProgress(Model model, HttpServletRequest request,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Pageable pageable) {

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
            

            model.addAttribute("nCuestionariosCompletados", cuestionariosCompletados.size());
            model.addAttribute("puntuacionMedia", Math.round(puntuacionMedia));
            model.addAttribute("nAsignaturasActivas", nAsignaturasActivas);
            model.addAttribute("tiempoEstudio", Utils.formatDuration(totalSegundos));
            model.addAttribute("historialCuestionariosPage", 
            intentoCuestionarioService.getIntentosCuestionariosByUsuarioYEstadoPaginated(authUser.getId(), 
                IntentoCuestionarioEstadoEnum.FINISHED.toString(), pageable));
        }
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        return "progreso/mi-progreso";
    }

    @GetMapping("/miProgreso/historial-cuestionario/tableData")
    public String getHistorialCuestionariosTableData(Model model, Pageable pageable, @AuthenticationPrincipal QuizlyUserDetails authUser) {
        model.addAttribute("historialCuestionariosPage", 
            intentoCuestionarioService.getIntentosCuestionariosByUsuarioYEstadoPaginated(authUser.getId(), 
                IntentoCuestionarioEstadoEnum.FINISHED.toString(), pageable));
        
        return "progreso/mi-progreso :: historial-cuestionarios-table_fragment";
    }
    
    
}
