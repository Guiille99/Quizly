package com.quizly.app.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quizly.app.dto.ActividadRecienteDto;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.util.Utils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActividadService {

    private final IntentoCuestionarioRepository intentoCuestionarioRepository;
    private final Utils utils;

    public List<ActividadRecienteDto> getActividadesRecientes(Usuario usuario, int limite) {
        List<String> codigosEstadoIntento = List.of(IntentoCuestionarioEstadoEnum.IN_PROGRESS.name(), IntentoCuestionarioEstadoEnum.FINISHED.name());

        List<IntentoCuestionario> intentos = intentoCuestionarioRepository.findLatestByUsuarioIdAndEstado(usuario.getId(), 
            codigosEstadoIntento, limite);

        return intentos.stream().map(this::toActividadRecienteDto).toList();
    }

    /**
     * Mapea el objeto {@link IntentoCuestionario} recibido
     * a {@link ActividadRecienteDto}
     * @param intento
     * @return {@link ActividadRecienteDto}
     */
    private ActividadRecienteDto toActividadRecienteDto(IntentoCuestionario intento) {
        String estado = intento.getEstado().getCodigo();
        
        String titulo = "";
        String subtitulo = "";
        String tipo = "";
        String bgColorClass = "";
        String textColorClass = "";
        String fechaRelativa = "";

        if (IntentoCuestionarioEstadoEnum.FINISHED.toString().equals(estado)) {
            fechaRelativa = utils.formatFechaRelativa(intento.getUpdatedAt());

            // Si el cuestionario ha sido completado, comprobamos si se trata de un nuevo récord
            Double mejorPrevio = intentoCuestionarioRepository.findMaxPorcentajeAntesDe(
                intento.getUsuario().getId(), intento.getCuestionario().getId(), intento.getId());

            if (mejorPrevio != -1 && intento.getPorcentajeAciertos() > mejorPrevio) {
                titulo = "Nuevo récord en " + intento.getCuestionario().getTitulo();
                subtitulo = Math.round(intento.getPorcentajeAciertos()) + "%";
                tipo = "RECORD";
                bgColorClass = "bg-accent-orange-muted";
                textColorClass = "text-accent-orange";
            } else {
                titulo = "Completaste " + intento.getCuestionario().getTitulo();
                subtitulo = intento.getCuestionario().getDificultad().getNombre();
                tipo = "COMPLETADO";
                bgColorClass = "bg-accent-green-muted";
                textColorClass = "text-accent-green";
            }
        } else if (IntentoCuestionarioEstadoEnum.IN_PROGRESS.toString().equals(estado)) {
            fechaRelativa = utils.formatFechaRelativa(intento.getFechaInicio());

            titulo = "Comenzaste tema " + intento.getCuestionario().getTema().getNombre();
            subtitulo = intento.getCuestionario().getTema().getAsignatura().getNombre();
            tipo = "INICIADO";
            bgColorClass = "bg-accent-purple-muted";
            textColorClass = "text-accent-purple";
        }

        return ActividadRecienteDto.builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .fechaRelativa(fechaRelativa)
                .tipo(tipo)
                .bgColorClass(bgColorClass)
                .textColorClass(textColorClass)
                .build();
    }


}
