package com.quizly.app.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.config.QuizProperties;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.model.session.UserQuizSession;
import com.quizly.app.repository.IntentoCuestionarioEstadoRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntentoCuestionarioService {
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;
    private final IntentoCuestionarioEstadoRepository intentoCuestionarioEstadoRepository;
    private final QuizProperties quizProperties;

    public Page<IntentoCuestionario> getIntentosCuestionariosByUsuarioYEstadoPaginated(Long userId, String codigoEstadoIntento, 
            Pageable pageable){
        return intentoCuestionarioRepository.getIntentosCuestionariosByUsuarioYEstado(userId, codigoEstadoIntento, pageable);
    }

    public IntentoCuestionario crearIntentoCuestionario(Cuestionario cuestionario, Usuario usuario){
        IntentoCuestionario intentoCuestionario = new IntentoCuestionario();
        
        intentoCuestionario.setCuestionario(cuestionario);
        intentoCuestionario.setUsuario(usuario);
        intentoCuestionario.setFechaInicio(LocalDateTime.now());
        intentoCuestionario.actualizarUltimaActividad();
        intentoCuestionario.setAciertos(0);
        intentoCuestionario.setFallos(0);
        intentoCuestionario.setTiempoEmpleado(0L);
        intentoCuestionario.setEstado(intentoCuestionarioEstadoRepository.findByCodigo(IntentoCuestionarioEstadoEnum.IN_PROGRESS.toString()).get());

        return intentoCuestionarioRepository.save(intentoCuestionario);
    }

    /**
     * Actualiza la fecha de la última actividad de un intento de cuestionario.
     * @param quizSession
     */
    @Transactional
    public void actualizarFechaUltimaActividad(UserQuizSession quizSession){
        LocalDateTime ahora = LocalDateTime.now();

        if (ahora.isBefore(quizSession.getFechaUltimaActividad().plus(quizProperties.getActivityUpdateInterval()))) {
            return;
        }

        intentoCuestionarioRepository.actualizarFechaUltimaActividad(quizSession.getIntentoCuestionarioId(), ahora);

        quizSession.setFechaUltimaActividad(ahora);
    }

    /**
     * Devuelve el intento en progreso del usuario para el cuestionario indicado,
     * siempre que todavía pueda reanudarse.
     * Si el intento existe pero ya no es reanudable, se marca automáticamente
     * como abandonado y de devuelve un {@link Optional#empty()}.
     * @param userId
     * @param slug
     * @return
     */
    public Optional<IntentoCuestionario> getIntentoReanudable(Long userId, String slug){
        Optional<IntentoCuestionario> intento = intentoCuestionarioRepository
            .findIntentoAbiertoByUsuarioYSlug(userId, slug);

        if (intento.isEmpty()) {
            return Optional.empty();
        }

        Cuestionario cuestionario = intento.get().getCuestionario();
        if (cuestionario.getUsuario() != null && !userId.equals(cuestionario.getUsuario().getId())) {
            return Optional.empty();
        }

        if (!intento.get().puedeReanudarse(quizProperties.getInactivityMaxDuration())) {
            abandonarIntento(intento.get());
            return Optional.empty();
        }

        return intento;
    }

    /**
     * Marca un intento como abandonado y registra su fecha de finalización 
     * y el tiempo empleado
     * @param intento
     * @return
     */
    @Transactional
    public IntentoCuestionario abandonarIntento(IntentoCuestionario intento){
        if (IntentoCuestionarioEstadoEnum.ABANDONNED.name().equals(intento.getEstado().getCodigo())) {
            return intento;
        }

        LocalDateTime fechaFin = LocalDateTime.now();

        intento.setFechaFin(fechaFin);
        intento.setTiempoEmpleado(Duration.between(intento.getFechaInicio(), fechaFin).toSeconds());

        intento.setEstado(intentoCuestionarioEstadoRepository
            .findByCodigo(IntentoCuestionarioEstadoEnum.ABANDONNED.name())
            .orElseThrow()
        );
        
        return intentoCuestionarioRepository.save(intento);
    }

    /**
     * Obtiene un intento de cuestionario a partir de su identificador y verifica
     * que continúa siendo válido.
     * Si el intento está en progreso pero ya no puede reanudarse, se marca 
     * automáticamente como abandonado antes de devolverlo.
     * @param intentoId
     * @return
     */
    @Transactional
    public IntentoCuestionario getIntentoActivo(Long intentoId){
        IntentoCuestionario intento = intentoCuestionarioRepository.findById(intentoId)
            .orElseThrow(() -> new EntityNotFoundException("No existe el intento del cuestionario."));

        // Si ya está finalizado o abandonado no hacemos ninguna comprobación
        if (IntentoCuestionarioEstadoEnum.FINISHED.name().equals(intento.getEstado().getCodigo())
            || IntentoCuestionarioEstadoEnum.ABANDONNED.name().equals(intento.getEstado().getCodigo())) {
            return  intento;
        }

        // Si ya no puede reanudarse, lo marcamos como abandonado
        if (!intento.puedeReanudarse(quizProperties.getInactivityMaxDuration())) {
            return abandonarIntento(intento);
        }

        return intento;
    }

    /**
     * Recorre todos los intentos en progreso que han superado el tiempo de inactividad
     * máximo (cuestionarios sin tiempo límite) o cuyo tiempo límite ha expirado
     * (cuestionarios con tiempo límite) y los marca como abandonados.
     * <p>
     * Este método está pensado para ser invocado periódicamente por un job programado,
     * de modo que los intentos queden cerrados aunque el usuario nunca vuelva a
     * interactuar con la aplicación.
     *
     * @return Número total de intentos cerrados
     */
    @Transactional
    public int cerrarIntentosExpirados() {
        LocalDateTime ahora = LocalDateTime.now();

        // Intentos sin tiempo límite cuya inactividad ha caducado
        LocalDateTime umbralInactividad = ahora.minus(quizProperties.getInactivityMaxDuration());
        List<IntentoCuestionario> intentosPorInactividad =
            intentoCuestionarioRepository.findIntentosEnProgresoConInactividadCaducada(umbralInactividad);

        // Intentos con tiempo límite que ya han expirado
        List<IntentoCuestionario> intentosPorTiempoLimite =
            intentoCuestionarioRepository.findIntentosEnProgresoConTiempoAgotado(ahora);

        int cerrados = 0;

        for (IntentoCuestionario intento : intentosPorInactividad) {
            abandonarIntento(intento);
            cerrados++;
        }

        for (IntentoCuestionario intento : intentosPorTiempoLimite) {
            abandonarIntento(intento);
            cerrados++;
        }

        return cerrados;
    }
}
