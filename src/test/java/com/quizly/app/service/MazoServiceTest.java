package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quizly.app.dto.ai.MazoGenerado;
import com.quizly.app.dto.ai.TarjetaGenerada;
import com.quizly.app.exception.MazoNotFoundException;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.CuestionarioSerie;
import com.quizly.app.model.entity.Mazo;
import com.quizly.app.model.entity.OpcionRespuesta;
import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.model.entity.Tarjeta;
import com.quizly.app.model.enums.Calificacion;
import com.quizly.app.model.view.tarjeta.EstadoEstudio;
import com.quizly.app.model.view.tarjeta.FalloPregunta;
import com.quizly.app.model.view.tarjeta.ResultadoImportacion;
import com.quizly.app.repository.MazoRepository;
import com.quizly.app.repository.PreguntaRepository;
import com.quizly.app.repository.TarjetaRepository;
import com.quizly.app.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MazoServiceTest {

    private static final Long USUARIO = 7L;
    private static final Long MAZO = 1L;
    private static final Long TARJETA = 5L;

    @Mock
    private MazoRepository mazoRepository;
    @Mock
    private TarjetaRepository tarjetaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PreguntaRepository preguntaRepository;
    @Mock
    private CuestionarioService cuestionarioService;
    @Spy
    private Sm2Scheduler sm2Scheduler = new Sm2Scheduler();

    @InjectMocks
    private MazoService mazoService;

    @Test
    @DisplayName("Un mazo ajeno o inexistente lanza MazoNotFoundException")
    void mazoAjeno() {
        when(mazoRepository.findByIdAndUsuarioId(MAZO, USUARIO)).thenReturn(Optional.empty());

        assertThrows(MazoNotFoundException.class, () -> mazoService.obtener(MAZO, USUARIO));
        assertThrows(MazoNotFoundException.class, () -> mazoService.anadirTarjeta(MAZO, USUARIO, "a", "b"));
        assertThrows(MazoNotFoundException.class, () -> mazoService.eliminar(MAZO, USUARIO));
        verify(tarjetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una tarjeta que no pertenece al mazo/usuario indicado no se puede editar, borrar ni calificar")
    void tarjetaAjena() {
        when(tarjetaRepository.findByIdAndMazoIdAndMazoUsuarioId(TARJETA, MAZO, USUARIO)).thenReturn(Optional.empty());

        assertThrows(MazoNotFoundException.class, () -> mazoService.editarTarjeta(MAZO, TARJETA, USUARIO, "a", "b"));
        assertThrows(MazoNotFoundException.class, () -> mazoService.eliminarTarjeta(MAZO, TARJETA, USUARIO));
        assertThrows(MazoNotFoundException.class,
                () -> mazoService.calificar(MAZO, TARJETA, Calificacion.BIEN, USUARIO));
        verify(tarjetaRepository, never()).save(any());
        verify(tarjetaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("El mazo de fallos no se puede eliminar")
    void mazoDeFallosNoSeElimina() {
        Mazo fallos = new Mazo();
        fallos.setDeFallos(true);
        when(mazoRepository.findByIdAndUsuarioId(MAZO, USUARIO)).thenReturn(Optional.of(fallos));

        assertFalse(mazoService.eliminar(MAZO, USUARIO));
        verify(mazoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("El mazo de fallos conserva su título al actualizarlo")
    void mazoDeFallosConservaTitulo() {
        Mazo fallos = new Mazo();
        fallos.setDeFallos(true);
        fallos.setTitulo(MazoService.TITULO_MAZO_FALLOS);
        when(mazoRepository.findByIdAndUsuarioId(MAZO, USUARIO)).thenReturn(Optional.of(fallos));
        when(mazoRepository.save(fallos)).thenReturn(fallos);

        mazoService.actualizar(MAZO, USUARIO, "Otro nombre", "  desc  ");

        assertEquals(MazoService.TITULO_MAZO_FALLOS, fallos.getTitulo());
        assertEquals("desc", fallos.getDescripcion());
    }

    @Test
    @DisplayName("Al guardar una tarjeta se normaliza el texto")
    void anadirTarjetaNormaliza() {
        when(mazoRepository.findByIdAndUsuarioId(MAZO, USUARIO)).thenReturn(Optional.of(new Mazo()));
        when(tarjetaRepository.save(any(Tarjeta.class))).thenAnswer(i -> i.getArgument(0));

        Tarjeta t = mazoService.anadirTarjeta(MAZO, USUARIO, "  ¿Qué   es\nJava? ", "x".repeat(1200));

        assertEquals("¿Qué es Java?", t.getAnverso());
        assertEquals(1000, t.getReverso().length());
    }

    @Test
    @DisplayName("Guardar cambios actualiza el mazo, edita las tarjetas y añade la nueva en un solo paso")
    void guardarCambiosCompleto() {
        Mazo mazo = new Mazo();
        Tarjeta existente = new Tarjeta();
        when(mazoRepository.findByIdAndUsuarioId(MAZO, USUARIO)).thenReturn(Optional.of(mazo));
        when(tarjetaRepository.findByIdAndMazoIdAndMazoUsuarioId(TARJETA, MAZO, USUARIO))
                .thenReturn(Optional.of(existente));
        when(tarjetaRepository.save(any(Tarjeta.class))).thenAnswer(i -> i.getArgument(0));

        mazoService.guardarCambios(MAZO, USUARIO, " Título ", null, List.of(TARJETA), List.of(" A  1 "), List.of("B1"),
                "Nueva", "Resp");

        assertEquals("Título", mazo.getTitulo());
        assertEquals("A 1", existente.getAnverso());
        ArgumentCaptor<Tarjeta> guardadas = ArgumentCaptor.forClass(Tarjeta.class);
        verify(tarjetaRepository, org.mockito.Mockito.times(2)).save(guardadas.capture());
        assertEquals("Nueva", guardadas.getAllValues().get(1).getAnverso());
    }

    @Test
    @DisplayName("Guardar cambios sin tarjeta nueva no crea ninguna")
    void guardarCambiosSinNueva() {
        when(mazoRepository.findByIdAndUsuarioId(MAZO, USUARIO)).thenReturn(Optional.of(new Mazo()));

        mazoService.guardarCambios(MAZO, USUARIO, "T", null, null, null, null, "  ", null);

        verify(tarjetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guardar cambios con datos inválidos no guarda nada")
    void guardarCambiosInvalido() {
        // Tarjeta nueva a medias, anverso vacío y listas de distinto tamaño
        assertThrows(IllegalArgumentException.class, () -> mazoService.guardarCambios(MAZO, USUARIO, "T", null, null,
                null, null, "solo anverso", ""));
        assertThrows(IllegalArgumentException.class, () -> mazoService.guardarCambios(MAZO, USUARIO, "T", null,
                List.of(TARJETA), List.of(" "), List.of("b"), null, null));
        assertThrows(IllegalArgumentException.class, () -> mazoService.guardarCambios(MAZO, USUARIO, "T", null,
                List.of(TARJETA), List.of("a"), List.of(), null, null));

        verify(mazoRepository, never()).save(any());
        verify(tarjetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Calificar una tarjeta pendiente aplica SM-2 y devuelve la siguiente")
    void calificarPendiente() {
        Tarjeta t = new Tarjeta();
        t.setProximaRevision(LocalDateTime.now().minusMinutes(1));
        when(tarjetaRepository.findByIdAndMazoIdAndMazoUsuarioId(TARJETA, MAZO, USUARIO)).thenReturn(Optional.of(t));

        EstadoEstudio estado = mazoService.calificar(MAZO, TARJETA, Calificacion.BIEN, USUARIO);

        assertEquals(1, t.getRepeticiones());
        assertTrue(t.getProximaRevision().isAfter(LocalDateTime.now()));
        verify(tarjetaRepository).save(t);
        assertNull(estado.tarjeta());
    }

    @Test
    @DisplayName("Calificar dos veces la misma tarjeta (doble envío) no reaplica SM-2")
    void calificarNoPendienteNoHaceNada() {
        Tarjeta t = new Tarjeta();
        t.setRepeticiones(1);
        t.setIntervaloDias(1);
        t.setProximaRevision(LocalDateTime.now().plusDays(1));
        when(tarjetaRepository.findByIdAndMazoIdAndMazoUsuarioId(TARJETA, MAZO, USUARIO)).thenReturn(Optional.of(t));

        mazoService.calificar(MAZO, TARJETA, Calificacion.BIEN, USUARIO);

        assertEquals(1, t.getRepeticiones());
        assertEquals(1, t.getIntervaloDias());
        verify(tarjetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sin pendientes pero con una tarjeta en la ventana de 10 min se informa de la espera")
    void esperaDentroDeLaVentana() {
        Tarjeta proxima = new Tarjeta();
        proxima.setProximaRevision(LocalDateTime.now().plusMinutes(5));
        when(mazoRepository.findByIdAndUsuarioId(MAZO, USUARIO)).thenReturn(Optional.of(new Mazo()));
        when(tarjetaRepository.findFirstByMazoIdAndProximaRevisionLessThanEqualOrderByProximaRevisionAscIdAsc(
                anyLong(), any())).thenReturn(Optional.empty());
        when(tarjetaRepository.findFirstByMazoIdAndProximaRevisionAfterOrderByProximaRevisionAscIdAsc(
                anyLong(), any())).thenReturn(Optional.of(proxima));

        EstadoEstudio estado = mazoService.siguiente(MAZO, USUARIO);

        assertNull(estado.tarjeta());
        assertTrue(estado.minutosEspera() >= 4 && estado.minutosEspera() <= 5);
    }

    @Test
    @DisplayName("crearDesdeIa guarda el mazo del usuario con todas sus tarjetas")
    void crearDesdeIa() {
        when(mazoRepository.save(any(Mazo.class))).thenAnswer(i -> i.getArgument(0));

        Mazo mazo = mazoService.crearDesdeIa(USUARIO, new MazoGenerado("Redes",
                List.of(new TarjetaGenerada("¿TCP?", "Fiable"), new TarjetaGenerada("¿UDP?", "Sin conexión"))));

        assertEquals("Redes", mazo.getTitulo());
        assertFalse(mazo.isDeFallos());
        ArgumentCaptor<List<Tarjeta>> captor = ArgumentCaptor.forClass(List.class);
        verify(tarjetaRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertSame(mazo, captor.getValue().get(0).getMazo());
        assertEquals("Sin conexión", captor.getValue().get(1).getReverso());
    }

    // ---------------------------------------------------------------------
    // Importación desde cuestionario
    // ---------------------------------------------------------------------

    private OpcionRespuesta opcion(String texto, boolean correcta, String retro) {
        OpcionRespuesta o = new OpcionRespuesta();
        o.setTexto(texto);
        o.setCorrecta(correcta);
        o.setRetroalimentacion(retro);
        return o;
    }

    private Pregunta pregunta(Long id, String enunciado, OpcionRespuesta... opciones) {
        Pregunta p = new Pregunta();
        p.setId(id);
        p.setEnunciado(enunciado);
        p.setOpcionesRespuesta(List.of(opciones));
        return p;
    }

    @Test
    @DisplayName("Importar crea una tarjeta por pregunta, omite sin correcta o ya existentes y reutiliza el mazo de la serie")
    void importarDesdeCuestionario() {
        CuestionarioSerie serie = new CuestionarioSerie();
        serie.setId(3L);
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setTitulo("Java");
        cuestionario.setSerie(serie);
        cuestionario.setPreguntas(List.of(
                pregunta(1L, "¿Qué es   Java?", opcion("Un lenguaje", true, "Compilado a bytecode"), opcion("Un café", false, null)),
                pregunta(2L, "Sin correcta", opcion("x", false, null)),
                pregunta(3L, "Ya existe", opcion("y", true, null))));
        Mazo mazo = new Mazo();
        mazo.setId(10L);

        when(cuestionarioService.getLastAccessibleVersionQuizBySlug("java", USUARIO)).thenReturn(Optional.of(cuestionario));
        when(mazoRepository.findByUsuarioIdAndSerieId(USUARIO, 3L)).thenReturn(Optional.of(mazo));
        when(tarjetaRepository.findAnversosByMazoId(10L)).thenReturn(List.of("YA  existe"));

        ResultadoImportacion resultado = mazoService.importarDesdeCuestionario("java", USUARIO);

        assertEquals(1, resultado.importadas());
        assertEquals(1, resultado.sinRespuesta());
        assertEquals(1, resultado.yaExistentes());
        assertEquals(10L, resultado.mazoId());
        verify(mazoRepository).insertMazoSerieIfAbsent(USUARIO, 3L, "Java");

        ArgumentCaptor<List<Tarjeta>> captor = ArgumentCaptor.forClass(List.class);
        verify(tarjetaRepository).saveAll(captor.capture());
        Tarjeta creada = captor.getValue().get(0);
        assertEquals("¿Qué es Java?", creada.getAnverso());
        assertEquals("Un lenguaje — Compilado a bytecode", creada.getReverso());
        assertEquals(1L, creada.getPregunta().getId());
    }

    @Test
    @DisplayName("Si ninguna pregunta tiene respuesta correcta no se crea el mazo")
    void importarSinNingunaRespuestaCorrectaNoCreaMazo() {
        CuestionarioSerie serie = new CuestionarioSerie();
        serie.setId(3L);
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setSerie(serie);
        cuestionario.setPreguntas(List.of(
                pregunta(1L, "Sin opciones"),
                pregunta(2L, "Sin correcta", opcion("x", false, null))));
        when(cuestionarioService.getLastAccessibleVersionQuizBySlug("vacio", USUARIO)).thenReturn(Optional.of(cuestionario));

        ResultadoImportacion resultado = mazoService.importarDesdeCuestionario("vacio", USUARIO);

        assertNull(resultado.mazoId());
        assertEquals(0, resultado.importadas());
        assertEquals(2, resultado.sinRespuesta());
        verify(mazoRepository, never()).insertMazoSerieIfAbsent(anyLong(), anyLong(), any());
        verify(tarjetaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Importar un cuestionario inaccesible lanza MazoNotFoundException")
    void importarCuestionarioInaccesible() {
        when(cuestionarioService.getLastAccessibleVersionQuizBySlug("x", USUARIO)).thenReturn(Optional.empty());

        assertThrows(MazoNotFoundException.class, () -> mazoService.importarDesdeCuestionario("x", USUARIO));
    }

    // ---------------------------------------------------------------------
    // Fallos en cuestionarios
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Un fallo reprograma a 'ahora' la tarjeta que ya existe para esa pregunta, sin crear el mazo de fallos")
    void registrarFalloTarjetaExistente() {
        Pregunta pregunta = new Pregunta();
        pregunta.setId(1L);
        Tarjeta existente = new Tarjeta();
        existente.setPregunta(pregunta);
        existente.setRepeticiones(4);
        existente.setIntervaloDias(20);
        existente.setProximaRevision(LocalDateTime.now().plusDays(20));
        when(tarjetaRepository.findByMazoUsuarioIdAndPreguntaIdIn(USUARIO, Set.of(1L))).thenReturn(List.of(existente));

        mazoService.registrarFallos(USUARIO, List.of(new FalloPregunta(1L, "a", "b")));

        assertEquals(0, existente.getRepeticiones());
        assertFalse(existente.getProximaRevision().isAfter(LocalDateTime.now()), "pendiente de inmediato, no +10 min");
        verify(mazoRepository, never()).insertMazoFallosIfAbsent(anyLong(), any());
        verify(tarjetaRepository).saveAll(List.of(existente));
    }

    @Test
    @DisplayName("Un fallo sin tarjeta la crea en el mazo de fallos, enlazada a la pregunta")
    void registrarFalloCreaTarjeta() {
        Mazo mazoFallos = new Mazo();
        mazoFallos.setId(99L);
        Pregunta referencia = new Pregunta();
        referencia.setId(2L);
        when(tarjetaRepository.findByMazoUsuarioIdAndPreguntaIdIn(USUARIO, Set.of(2L))).thenReturn(List.of());
        when(mazoRepository.findByUsuarioIdAndDeFallosTrue(USUARIO)).thenReturn(Optional.of(mazoFallos));
        when(tarjetaRepository.findByMazoIdOrderByIdAsc(99L)).thenReturn(List.of());
        when(preguntaRepository.getReferenceById(2L)).thenReturn(referencia);

        mazoService.registrarFallos(USUARIO, List.of(new FalloPregunta(2L, "  ¿Capital   de Francia? ", "París")));

        verify(mazoRepository).insertMazoFallosIfAbsent(USUARIO, MazoService.TITULO_MAZO_FALLOS);
        ArgumentCaptor<List<Tarjeta>> captor = ArgumentCaptor.forClass(List.class);
        verify(tarjetaRepository).saveAll(captor.capture());
        Tarjeta creada = captor.getValue().get(0);
        assertSame(mazoFallos, creada.getMazo());
        assertEquals("¿Capital de Francia?", creada.getAnverso());
        assertEquals("París", creada.getReverso());
        assertSame(referencia, creada.getPregunta());
        assertFalse(creada.getProximaRevision().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Si el mazo de fallos ya tiene el mismo anverso (otra versión del cuestionario) se reutiliza, no se duplica")
    void registrarFalloReutilizaPorAnverso() {
        Mazo mazoFallos = new Mazo();
        mazoFallos.setId(99L);
        Tarjeta previa = new Tarjeta();
        previa.setAnverso("¿Capital de Francia?");
        previa.setRepeticiones(3);
        Pregunta nueva = new Pregunta();
        nueva.setId(50L);
        when(tarjetaRepository.findByMazoUsuarioIdAndPreguntaIdIn(USUARIO, Set.of(50L))).thenReturn(List.of());
        when(mazoRepository.findByUsuarioIdAndDeFallosTrue(USUARIO)).thenReturn(Optional.of(mazoFallos));
        when(tarjetaRepository.findByMazoIdOrderByIdAsc(99L)).thenReturn(List.of(previa));
        when(preguntaRepository.getReferenceById(50L)).thenReturn(nueva);

        mazoService.registrarFallos(USUARIO, List.of(new FalloPregunta(50L, "¿capital de francia?", "París")));

        assertEquals(0, previa.getRepeticiones());
        assertSame(nueva, previa.getPregunta());
        verify(tarjetaRepository).saveAll(List.of(previa));
    }

    @Test
    @DisplayName("Sin fallos no se toca nada")
    void registrarSinFallos() {
        mazoService.registrarFallos(USUARIO, List.of());

        verify(tarjetaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Si la siguiente tarjeta es de otro día, no hay espera: 'terminado por hoy'")
    void sinEsperaSiLaSiguienteEsMasTarde() {
        Tarjeta manana = new Tarjeta();
        manana.setProximaRevision(LocalDateTime.now().plusDays(1));
        when(mazoRepository.findByIdAndUsuarioId(MAZO, USUARIO)).thenReturn(Optional.of(new Mazo()));
        when(tarjetaRepository.findFirstByMazoIdAndProximaRevisionLessThanEqualOrderByProximaRevisionAscIdAsc(
                anyLong(), any())).thenReturn(Optional.empty());
        when(tarjetaRepository.findFirstByMazoIdAndProximaRevisionAfterOrderByProximaRevisionAscIdAsc(
                anyLong(), any())).thenReturn(Optional.of(manana));

        assertNull(mazoService.siguiente(MAZO, USUARIO).minutosEspera());
    }
}
