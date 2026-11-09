package com.quizly.app.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.quizly.app.dto.ai.MazoGenerado;
import com.quizly.app.dto.ai.TarjetaGenerada;
import com.quizly.app.exception.MazoNotFoundException;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Mazo;
import com.quizly.app.model.entity.OpcionRespuesta;
import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.model.entity.Tarjeta;
import com.quizly.app.model.enums.Calificacion;
import com.quizly.app.model.view.tarjeta.EstadoEstudio;
import com.quizly.app.model.view.tarjeta.FalloPregunta;
import com.quizly.app.model.view.tarjeta.MazoResumen;
import com.quizly.app.model.view.tarjeta.ResultadoImportacion;
import com.quizly.app.repository.MazoRepository;
import com.quizly.app.repository.PreguntaRepository;
import com.quizly.app.repository.TarjetaRepository;
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.util.TextoTarjeta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MazoService {

    public static final String TITULO_MAZO_FALLOS = "Repaso de fallos";

    private final MazoRepository mazoRepository;
    private final TarjetaRepository tarjetaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PreguntaRepository preguntaRepository;
    private final CuestionarioService cuestionarioService;
    private final Sm2Scheduler sm2Scheduler;

    // =========================================================================
    // Mazos
    // =========================================================================

    @Transactional(readOnly = true)
    public List<MazoResumen> listar(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        return mazoRepository.findByUsuarioIdOrderByTitulo(usuarioId).stream()
                .map(m -> new MazoResumen(m,
                        tarjetaRepository.countByMazoId(m.getId()),
                        tarjetaRepository.countByMazoIdAndProximaRevisionLessThanEqual(m.getId(), ahora)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Mazo obtener(Long mazoId, Long usuarioId) {
        return mazoRepository.findByIdAndUsuarioId(mazoId, usuarioId).orElseThrow(MazoNotFoundException::new);
    }

    @Transactional
    public Mazo crear(Long usuarioId, String titulo, String descripcion) {
        Mazo mazo = new Mazo();
        mazo.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        mazo.setTitulo(titulo.strip());
        mazo.setDescripcion(textoOpcional(descripcion));
        return mazoRepository.save(mazo);
    }

    @Transactional
    public Mazo actualizar(Long mazoId, Long usuarioId, String titulo, String descripcion) {
        Mazo mazo = obtener(mazoId, usuarioId);
        // El mazo automático de fallos conserva su nombre: así se reconoce y no se duplica
        if (!mazo.isDeFallos()) {
            mazo.setTitulo(titulo.strip());
        }
        mazo.setDescripcion(textoOpcional(descripcion));
        return mazoRepository.save(mazo);
    }

    /**
     * Guarda en una sola transacción el mazo, las ediciones de sus tarjetas y, si viene, una tarjeta nueva.
     * Si algo no es válido no se guarda nada.
     *
     * @throws IllegalArgumentException con un mensaje apto para mostrar al usuario
     */
    @Transactional
    public void guardarCambios(Long mazoId, Long usuarioId, String titulo, String descripcion, List<Long> ids,
            List<String> anversos, List<String> reversos, String nuevoAnverso, String nuevoReverso) {
        int n = ids == null ? 0 : ids.size();
        if ((anversos == null ? 0 : anversos.size()) != n || (reversos == null ? 0 : reversos.size()) != n) {
            throw new IllegalArgumentException("Los datos de las tarjetas no son válidos. Recarga la página.");
        }
        for (int i = 0; i < n; i++) {
            if (anversos.get(i).isBlank() || reversos.get(i).isBlank()) {
                throw new IllegalArgumentException("Ninguna tarjeta puede quedarse sin anverso o sin reverso.");
            }
        }
        boolean hayAnverso = nuevoAnverso != null && !nuevoAnverso.isBlank();
        boolean hayReverso = nuevoReverso != null && !nuevoReverso.isBlank();
        if (hayAnverso != hayReverso) {
            throw new IllegalArgumentException("Para añadir una tarjeta rellena el anverso y el reverso, o deja ambos vacíos.");
        }

        actualizar(mazoId, usuarioId, titulo, descripcion);
        for (int i = 0; i < n; i++) {
            editarTarjeta(mazoId, ids.get(i), usuarioId, anversos.get(i), reversos.get(i));
        }
        if (hayAnverso) {
            anadirTarjeta(mazoId, usuarioId, nuevoAnverso, nuevoReverso);
        }
    }

    public Mazo crearDesdeIa(Long usuarioId, MazoGenerado generado) {
        return crearConTarjetas(usuarioId, generado.titulo(), "Generado con IA a partir de un PDF.",
                generado.tarjetas());
    }

    /**
     * Guarda el mazo y todas sus tarjetas en una sola transacción: si algo falla no queda un mazo a medias.
     * Las tarjetas deben llegar ya normalizadas a las longitudes de las columnas (ver {@code TextoTarjeta}).
     */
    @Transactional
    public Mazo crearConTarjetas(Long usuarioId, String titulo, String descripcion, List<TarjetaGenerada> tarjetas) {
        Mazo mazo = crear(usuarioId, titulo, descripcion);
        tarjetaRepository.saveAll(tarjetas.stream().map(t -> {
            Tarjeta tarjeta = new Tarjeta();
            tarjeta.setMazo(mazo);
            tarjeta.setAnverso(t.anverso());
            tarjeta.setReverso(t.reverso());
            return tarjeta;
        }).toList());
        return mazo;
    }

    /** @return {@code false} si es el mazo de fallos, que no se puede borrar */
    @Transactional
    public boolean eliminar(Long mazoId, Long usuarioId) {
        Mazo mazo = obtener(mazoId, usuarioId);
        if (mazo.isDeFallos()) {
            return false;
        }
        mazoRepository.delete(mazo);
        return true;
    }

    // =========================================================================
    // Tarjetas
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Tarjeta> tarjetas(Long mazoId, Long usuarioId) {
        obtener(mazoId, usuarioId);
        return tarjetaRepository.findByMazoIdOrderByIdAsc(mazoId);
    }

    @Transactional
    public Tarjeta anadirTarjeta(Long mazoId, Long usuarioId, String anverso, String reverso) {
        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setMazo(obtener(mazoId, usuarioId));
        rellenar(tarjeta, anverso, reverso);
        return tarjetaRepository.save(tarjeta);
    }

    @Transactional
    public Tarjeta editarTarjeta(Long mazoId, Long tarjetaId, Long usuarioId, String anverso, String reverso) {
        Tarjeta tarjeta = obtenerTarjeta(mazoId, tarjetaId, usuarioId);
        rellenar(tarjeta, anverso, reverso);
        return tarjetaRepository.save(tarjeta);
    }

    @Transactional
    public void eliminarTarjeta(Long mazoId, Long tarjetaId, Long usuarioId) {
        tarjetaRepository.delete(obtenerTarjeta(mazoId, tarjetaId, usuarioId));
    }

    // =========================================================================
    // Importación y fallos
    // =========================================================================

    /**
     * Crea (o completa) el mazo de la serie del cuestionario con una tarjeta por pregunta.
     * Reimportar reutiliza el mazo y solo añade los anversos que aún no tiene.
     */
    @Transactional
    public ResultadoImportacion importarDesdeCuestionario(String slug, Long usuarioId) {
        Cuestionario cuestionario = cuestionarioService.getLastAccessibleVersionQuizBySlug(slug, usuarioId)
                .orElseThrow(MazoNotFoundException::new);
        Long serieId = cuestionario.getSerie().getId();

        List<Pregunta> conRespuesta = cuestionario.getPreguntas().stream()
                .filter(p -> p.getOpcionesRespuesta().stream().anyMatch(o -> Boolean.TRUE.equals(o.getCorrecta())))
                .toList();
        int sinRespuesta = cuestionario.getPreguntas().size() - conRespuesta.size();
        if (conRespuesta.isEmpty()) {
            // Nada que importar: no se crea un mazo vacío
            return new ResultadoImportacion(null, 0, sinRespuesta, 0);
        }

        // Get-or-create atómico: un doble clic no produce un 500 por el índice único
        mazoRepository.insertMazoSerieIfAbsent(usuarioId, serieId,
                TextoTarjeta.normalizar(cuestionario.getTitulo(), 150));
        Mazo mazo = mazoRepository.findByUsuarioIdAndSerieId(usuarioId, serieId).orElseThrow();

        Set<String> existentes = tarjetaRepository.findAnversosByMazoId(mazo.getId()).stream()
                .map(TextoTarjeta::clave)
                .collect(Collectors.toCollection(HashSet::new));

        List<Tarjeta> nuevas = new ArrayList<>();
        int yaExistentes = 0;
        for (Pregunta pregunta : conRespuesta) {
            List<OpcionRespuesta> correctas = pregunta.getOpcionesRespuesta().stream()
                    .filter(o -> Boolean.TRUE.equals(o.getCorrecta()))
                    .toList();
            String anverso = TextoTarjeta.normalizar(pregunta.getEnunciado(), TextoTarjeta.MAX_ANVERSO);

            if (!existentes.add(TextoTarjeta.clave(anverso))) {
                yaExistentes++;
                continue;
            }
            String retroalimentacion = correctas.stream()
                    .map(OpcionRespuesta::getRetroalimentacion)
                    .filter(r -> r != null && !r.isBlank())
                    .findFirst()
                    .orElse(null);

            Tarjeta tarjeta = new Tarjeta();
            tarjeta.setMazo(mazo);
            tarjeta.setPregunta(pregunta);
            tarjeta.setAnverso(anverso);
            tarjeta.setReverso(TextoTarjeta.componerReverso(
                    correctas.stream().map(OpcionRespuesta::getTexto).toList(), retroalimentacion));
            nuevas.add(tarjeta);
        }
        tarjetaRepository.saveAll(nuevas);
        return new ResultadoImportacion(mazo.getId(), nuevas.size(), sinRespuesta, yaExistentes);
    }

    /**
     * Reprograma para repaso inmediato las tarjetas de las preguntas falladas y crea, en el mazo de
     * "Repaso de fallos", las que aún no tengan tarjeta. Transacción propia: se invoca tras el commit
     * del intento (ver {@code IntentoFinalizadoListener}).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallos(Long usuarioId, Collection<FalloPregunta> fallos) {
        if (fallos.isEmpty()) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        List<Tarjeta> tocadas = new ArrayList<>();

        Set<Long> preguntaIds = fallos.stream().map(FalloPregunta::preguntaId).collect(Collectors.toSet());
        Set<Long> cubiertas = new HashSet<>();
        for (Tarjeta tarjeta : tarjetaRepository.findByMazoUsuarioIdAndPreguntaIdIn(usuarioId, preguntaIds)) {
            sm2Scheduler.reprogramarPorFallo(tarjeta, ahora);
            cubiertas.add(tarjeta.getPregunta().getId());
            tocadas.add(tarjeta);
        }

        List<FalloPregunta> sinTarjeta = fallos.stream().filter(f -> !cubiertas.contains(f.preguntaId())).toList();
        if (!sinTarjeta.isEmpty()) {
            mazoRepository.insertMazoFallosIfAbsent(usuarioId, TITULO_MAZO_FALLOS);
            Mazo mazoFallos = mazoRepository.findByUsuarioIdAndDeFallosTrue(usuarioId).orElseThrow();

            // Por texto, no solo por pregunta: tras versionar un cuestionario los ids de pregunta cambian
            Map<String, Tarjeta> porAnverso = new HashMap<>();
            tarjetaRepository.findByMazoIdOrderByIdAsc(mazoFallos.getId())
                    .forEach(t -> porAnverso.putIfAbsent(TextoTarjeta.clave(t.getAnverso()), t));

            for (FalloPregunta fallo : sinTarjeta) {
                String anverso = TextoTarjeta.normalizar(fallo.anverso(), TextoTarjeta.MAX_ANVERSO);
                Tarjeta tarjeta = porAnverso.get(TextoTarjeta.clave(anverso));
                if (tarjeta == null) {
                    tarjeta = new Tarjeta();
                    tarjeta.setMazo(mazoFallos);
                    tarjeta.setAnverso(anverso);
                    tarjeta.setReverso(TextoTarjeta.normalizar(fallo.reverso(), TextoTarjeta.MAX_REVERSO));
                    tarjeta.setProximaRevision(ahora);
                    porAnverso.put(TextoTarjeta.clave(anverso), tarjeta);
                } else {
                    sm2Scheduler.reprogramarPorFallo(tarjeta, ahora);
                }
                tarjeta.setPregunta(preguntaRepository.getReferenceById(fallo.preguntaId()));
                tocadas.add(tarjeta);
            }
        }
        tarjetaRepository.saveAll(tocadas);
    }

    // =========================================================================
    // Estudio
    // =========================================================================

    @Transactional(readOnly = true)
    public EstadoEstudio siguiente(Long mazoId, Long usuarioId) {
        obtener(mazoId, usuarioId);
        return estado(mazoId, LocalDateTime.now());
    }

    /**
     * Aplica la calificación y devuelve el estado de estudio con la siguiente tarjeta.
     * Si la tarjeta ya no está pendiente (doble envío, otra pestaña) no se reaplica nada:
     * tras cualquier calificación su {@code proxima_revision} queda en el futuro.
     */
    @Transactional
    public EstadoEstudio calificar(Long mazoId, Long tarjetaId, Calificacion calificacion, Long usuarioId) {
        Tarjeta tarjeta = obtenerTarjeta(mazoId, tarjetaId, usuarioId);
        LocalDateTime ahora = LocalDateTime.now();

        if (!tarjeta.getProximaRevision().isAfter(ahora)) {
            sm2Scheduler.aplicar(tarjeta, calificacion, ahora);
            tarjetaRepository.save(tarjeta);
        } else {
            log.debug("Calificación ignorada: la tarjeta {} no está pendiente", tarjetaId);
        }
        return estado(mazoId, ahora);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private EstadoEstudio estado(Long mazoId, LocalDateTime ahora) {
        Tarjeta actual = tarjetaRepository
                .findFirstByMazoIdAndProximaRevisionLessThanEqualOrderByProximaRevisionAscIdAsc(mazoId, ahora)
                .orElse(null);
        long pendientes = tarjetaRepository.countByMazoIdAndProximaRevisionLessThanEqual(mazoId, ahora);
        long total = tarjetaRepository.countByMazoId(mazoId);

        Long espera = null;
        if (actual == null) {
            espera = tarjetaRepository
                    .findFirstByMazoIdAndProximaRevisionAfterOrderByProximaRevisionAscIdAsc(mazoId, ahora)
                    .filter(t -> !t.getProximaRevision().isAfter(ahora.plus(Sm2Scheduler.REPASO_INMEDIATO)))
                    .map(t -> Math.max(1, (Duration.between(ahora, t.getProximaRevision()).toSeconds() + 59) / 60))
                    .orElse(null);
        }
        return new EstadoEstudio(actual, pendientes, total, espera);
    }

    private Tarjeta obtenerTarjeta(Long mazoId, Long tarjetaId, Long usuarioId) {
        return tarjetaRepository.findByIdAndMazoIdAndMazoUsuarioId(tarjetaId, mazoId, usuarioId)
                .orElseThrow(MazoNotFoundException::new);
    }

    private void rellenar(Tarjeta tarjeta, String anverso, String reverso) {
        tarjeta.setAnverso(TextoTarjeta.normalizar(anverso, TextoTarjeta.MAX_ANVERSO));
        tarjeta.setReverso(TextoTarjeta.normalizar(reverso, TextoTarjeta.MAX_REVERSO));
    }

    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
