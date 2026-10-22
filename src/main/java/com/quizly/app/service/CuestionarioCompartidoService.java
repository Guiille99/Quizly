package com.quizly.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.CuestionarioCompartido;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.EstadoComparticion;
import com.quizly.app.repository.CuestionarioCompartidoRepository;
import com.quizly.app.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuestionarioCompartidoService {

    private final CuestionarioCompartidoRepository cuestionarioCompartidoRepository;
    private final CuestionarioService cuestionarioService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Envía una solicitud para compartir un cuestionario con otro usuario identificado por username o email.
     *
     * @param cuestionarioId        ID del cuestionario a compartir
     * @param destinatarioIdentificador username o email del receptor
     * @param emisorId              ID del usuario que realiza la compartición
     * @return entidad CuestionarioCompartido creada
     * @throws QuizlyException ante cualquier validación de negocio fallida
     */
    @Transactional
    public CuestionarioCompartido compartirCuestionario(Long cuestionarioId, String destinatarioIdentificador, Long emisorId) throws QuizlyException {
        if (destinatarioIdentificador == null || destinatarioIdentificador.trim().isEmpty()) {
            throw new QuizlyException("Debes indicar el nombre de usuario o correo electrónico del destinatario.");
        }

        String search = destinatarioIdentificador.trim();

        // 1. Validar que el cuestionario pertenece al emisor
        Cuestionario cuestionario = cuestionarioService.getPersonalQuizById(cuestionarioId, emisorId)
                .orElseThrow(() -> new QuizlyException("No tienes permiso para compartir este cuestionario o no existe."));

        // 2. Buscar al receptor por username o email
        Usuario receptor = usuarioRepository.findByUsername(search)
                .or(() -> usuarioRepository.findByEmail(search))
                .orElseThrow(() -> new QuizlyException("No se ha encontrado ningún usuario con ese nombre de usuario o correo electrónico."));

        // 3. Validar no compartir consigo mismo
        if (receptor.getId().equals(emisorId)) {
            throw new QuizlyException("No puedes compartir un cuestionario contigo mismo.");
        }

        // 4. Validar si ya está pendiente
        boolean yaPendiente = cuestionarioCompartidoRepository.existsByEmisorIdAndReceptorIdAndCuestionarioOrigenIdAndEstado(
                emisorId, receptor.getId(), cuestionarioId, EstadoComparticion.PENDIENTE);
        if (yaPendiente) {
            throw new QuizlyException("Ya has enviado una solicitud pendiente para este cuestionario a @" + receptor.getUsername() + ".");
        }

        // 5. Validar si ya lo ha aceptado previamente y aún lo conserva en su biblioteca
        boolean yaAceptado = cuestionarioCompartidoRepository.existsByReceptorIdAndCuestionarioOrigenIdAndEstadoAndCuestionarioClonadoIsNotNull(
                receptor.getId(), cuestionarioId, EstadoComparticion.ACEPTADO);
        if (yaAceptado) {
            throw new QuizlyException("El usuario @" + receptor.getUsername() + " ya dispone de este cuestionario en su biblioteca.");
        }

        // 6. Crear solicitud
        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new QuizlyException("Usuario emisor no encontrado."));

        CuestionarioCompartido compartido = new CuestionarioCompartido();
        compartido.setEmisor(emisor);
        compartido.setReceptor(receptor);
        compartido.setCuestionarioOrigen(cuestionario);
        compartido.setEstado(EstadoComparticion.PENDIENTE);
        compartido.setFechaEnvio(LocalDateTime.now());

        CuestionarioCompartido guardado = cuestionarioCompartidoRepository.save(compartido);
        log.info("Cuestionario {} compartido por usuario {} con usuario {}", cuestionarioId, emisorId, receptor.getId());
        return guardado;
    }

    /**
     * Obtiene las solicitudes de cuestionarios pendientes recibidas por un usuario.
     */
    @Transactional(readOnly = true)
    public List<CuestionarioCompartido> obtenerPendientesParaReceptor(Long receptorId) {
        return cuestionarioCompartidoRepository.findByReceptorIdAndEstadoOrderByFechaEnvioDesc(receptorId, EstadoComparticion.PENDIENTE);
    }

    /**
     * Cuenta el número de solicitudes pendientes recibidas por un usuario.
     */
    @Transactional(readOnly = true)
    public long contarPendientes(Long receptorId) {
        if (receptorId == null) {
            return 0;
        }
        return cuestionarioCompartidoRepository.countByReceptorIdAndEstado(receptorId, EstadoComparticion.PENDIENTE);
    }

    /**
     * Acepta un cuestionario compartido y genera un clon independiente para el usuario receptor.
     *
     * @param compartidoId ID de la solicitud compartida
     * @param receptorId   ID del usuario receptor autenticado
     * @return cuestionario clonado resultante
     * @throws QuizlyException si no existe o no tiene permiso
     */
    @Transactional
    public Cuestionario aceptarCompartido(Long compartidoId, Long receptorId) throws QuizlyException {
        CuestionarioCompartido compartido = cuestionarioCompartidoRepository.findById(compartidoId)
                .orElseThrow(() -> new QuizlyException("La solicitud no existe."));

        if (!compartido.getReceptor().getId().equals(receptorId)) {
            throw new QuizlyException("No tienes permiso para responder a esta solicitud.");
        }

        if (compartido.getEstado() != EstadoComparticion.PENDIENTE) {
            throw new QuizlyException("Esta solicitud ya ha sido respondida previamente.");
        }

        // Clonar cuestionario para el receptor
        Cuestionario clonado = cuestionarioService.clonarCuestionarioParaUsuario(
                compartido.getCuestionarioOrigen(), compartido.getReceptor());

        compartido.setEstado(EstadoComparticion.ACEPTADO);
        compartido.setCuestionarioClonado(clonado);
        compartido.setFechaRespuesta(LocalDateTime.now());
        cuestionarioCompartidoRepository.save(compartido);

        log.info("Usuario {} aceptó cuestionario compartido {}. Clon creado con id {}",
                receptorId, compartidoId, clonado.getId());
        return clonado;
    }

    /**
     * Rechaza una solicitud de cuestionario compartido.
     *
     * @param compartidoId ID de la solicitud
     * @param receptorId   ID del usuario receptor autenticado
     * @throws QuizlyException si no existe o no tiene permiso
     */
    @Transactional
    public void rechazarCompartido(Long compartidoId, Long receptorId) throws QuizlyException {
        CuestionarioCompartido compartido = cuestionarioCompartidoRepository.findById(compartidoId)
                .orElseThrow(() -> new QuizlyException("La solicitud no existe."));

        if (!compartido.getReceptor().getId().equals(receptorId)) {
            throw new QuizlyException("No tienes permiso para responder a esta solicitud.");
        }

        if (compartido.getEstado() != EstadoComparticion.PENDIENTE) {
            throw new QuizlyException("Esta solicitud ya ha sido respondida previamente.");
        }

        compartido.setEstado(EstadoComparticion.RECHAZADO);
        compartido.setFechaRespuesta(LocalDateTime.now());
        cuestionarioCompartidoRepository.save(compartido);

        log.info("Usuario {} rechazó cuestionario compartido {}", receptorId, compartidoId);
    }
}

