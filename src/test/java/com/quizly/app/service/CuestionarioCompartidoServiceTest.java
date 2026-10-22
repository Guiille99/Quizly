package com.quizly.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.CuestionarioCompartido;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.EstadoComparticion;
import com.quizly.app.repository.CuestionarioCompartidoRepository;
import com.quizly.app.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CuestionarioCompartidoServiceTest {

    @Mock
    private CuestionarioCompartidoRepository cuestionarioCompartidoRepository;

    @Mock
    private CuestionarioService cuestionarioService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CuestionarioCompartidoService compartidoService;

    private Usuario emisor;
    private Usuario receptor;
    private Cuestionario cuestionario;

    @BeforeEach
    void setUp() {
        emisor = new Usuario();
        emisor.setId(1L);
        emisor.setUsername("emisor");
        emisor.setEmail("emisor@quizly.com");

        receptor = new Usuario();
        receptor.setId(2L);
        receptor.setUsername("receptor");
        receptor.setEmail("receptor@quizly.com");

        cuestionario = new Cuestionario();
        cuestionario.setId(10L);
        cuestionario.setTitulo("Matemáticas Avanzadas");
        cuestionario.setUsuario(emisor);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el destinatario viene vacío o nulo")
    void compartir_destinatarioVacio_lanzaExcepcion() {
        assertThatThrownBy(() -> compartidoService.compartirCuestionario(10L, "   ", 1L))
                .isInstanceOf(QuizlyException.class)
                .hasMessageContaining("Debes indicar el nombre de usuario o correo electrónico");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el cuestionario no pertenece al emisor")
    void compartir_cuestionarioNoPropio_lanzaExcepcion() {
        when(cuestionarioService.getPersonalQuizById(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compartidoService.compartirCuestionario(10L, "receptor", 1L))
                .isInstanceOf(QuizlyException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el destinatario no existe")
    void compartir_destinatarioNoExiste_lanzaExcepcion() {
        when(cuestionarioService.getPersonalQuizById(10L, 1L)).thenReturn(Optional.of(cuestionario));
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compartidoService.compartirCuestionario(10L, "fantasma", 1L))
                .isInstanceOf(QuizlyException.class)
                .hasMessageContaining("No se ha encontrado ningún usuario");
    }

    @Test
    @DisplayName("Debe lanzar excepción si se intenta compartir consigo mismo")
    void compartir_consigoMismo_lanzaExcepcion() {
        when(cuestionarioService.getPersonalQuizById(10L, 1L)).thenReturn(Optional.of(cuestionario));
        when(usuarioRepository.findByUsername("emisor")).thenReturn(Optional.of(emisor));

        assertThatThrownBy(() -> compartidoService.compartirCuestionario(10L, "emisor", 1L))
                .isInstanceOf(QuizlyException.class)
                .hasMessageContaining("No puedes compartir un cuestionario contigo mismo");
    }

    @Test
    @DisplayName("Debe lanzar excepción si ya existe una solicitud PENDIENTE")
    void compartir_yaPendiente_lanzaExcepcion() {
        when(cuestionarioService.getPersonalQuizById(10L, 1L)).thenReturn(Optional.of(cuestionario));
        when(usuarioRepository.findByUsername("receptor")).thenReturn(Optional.of(receptor));
        when(cuestionarioCompartidoRepository.existsByEmisorIdAndReceptorIdAndCuestionarioOrigenIdAndEstado(
                1L, 2L, 10L, EstadoComparticion.PENDIENTE)).thenReturn(true);

        assertThatThrownBy(() -> compartidoService.compartirCuestionario(10L, "receptor", 1L))
                .isInstanceOf(QuizlyException.class)
                .hasMessageContaining("Ya has enviado una solicitud pendiente");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el receptor ya aceptó el cuestionario previamente y aún lo tiene")
    void compartir_yaAceptado_lanzaExcepcion() {
        when(cuestionarioService.getPersonalQuizById(10L, 1L)).thenReturn(Optional.of(cuestionario));
        when(usuarioRepository.findByUsername("receptor")).thenReturn(Optional.of(receptor));
        when(cuestionarioCompartidoRepository.existsByEmisorIdAndReceptorIdAndCuestionarioOrigenIdAndEstado(
                1L, 2L, 10L, EstadoComparticion.PENDIENTE)).thenReturn(false);
        when(cuestionarioCompartidoRepository.existsByReceptorIdAndCuestionarioOrigenIdAndEstadoAndCuestionarioClonadoIsNotNull(
                2L, 10L, EstadoComparticion.ACEPTADO)).thenReturn(true);

        assertThatThrownBy(() -> compartidoService.compartirCuestionario(10L, "receptor", 1L))
                .isInstanceOf(QuizlyException.class)
                .hasMessageContaining("ya dispone de este cuestionario");
    }

    @Test
    @DisplayName("Debe permitir volver a compartir si el receptor había aceptado pero luego eliminó el cuestionario")
    void compartir_previamenteAceptadoPeroEliminado_permiteRecompartir() throws QuizlyException {
        when(cuestionarioService.getPersonalQuizById(10L, 1L)).thenReturn(Optional.of(cuestionario));
        when(usuarioRepository.findByUsername("receptor")).thenReturn(Optional.of(receptor));
        when(cuestionarioCompartidoRepository.existsByEmisorIdAndReceptorIdAndCuestionarioOrigenIdAndEstado(
                1L, 2L, 10L, EstadoComparticion.PENDIENTE)).thenReturn(false);
        // Al haber sido borrado de la biblioteca del receptor, cuestionarioClonado es NULL -> devuelve false
        when(cuestionarioCompartidoRepository.existsByReceptorIdAndCuestionarioOrigenIdAndEstadoAndCuestionarioClonadoIsNotNull(
                2L, 10L, EstadoComparticion.ACEPTADO)).thenReturn(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(emisor));
        when(cuestionarioCompartidoRepository.save(any(CuestionarioCompartido.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioCompartido resultado = compartidoService.compartirCuestionario(10L, "receptor", 1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoComparticion.PENDIENTE);
        verify(cuestionarioCompartidoRepository).save(any(CuestionarioCompartido.class));
    }

    @Test
    @DisplayName("Debe crear la solicitud en estado PENDIENTE cuando todo es correcto (por email o username)")
    void compartir_exitoso_creaPendiente() throws QuizlyException {
        when(cuestionarioService.getPersonalQuizById(10L, 1L)).thenReturn(Optional.of(cuestionario));
        when(usuarioRepository.findByUsername("receptor@quizly.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("receptor@quizly.com")).thenReturn(Optional.of(receptor));
        when(cuestionarioCompartidoRepository.existsByEmisorIdAndReceptorIdAndCuestionarioOrigenIdAndEstado(
                1L, 2L, 10L, EstadoComparticion.PENDIENTE)).thenReturn(false);
        when(cuestionarioCompartidoRepository.existsByReceptorIdAndCuestionarioOrigenIdAndEstadoAndCuestionarioClonadoIsNotNull(
                2L, 10L, EstadoComparticion.ACEPTADO)).thenReturn(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(emisor));
        when(cuestionarioCompartidoRepository.save(any(CuestionarioCompartido.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CuestionarioCompartido resultado = compartidoService.compartirCuestionario(10L, "receptor@quizly.com", 1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoComparticion.PENDIENTE);
        assertThat(resultado.getEmisor()).isEqualTo(emisor);
        assertThat(resultado.getReceptor()).isEqualTo(receptor);
        assertThat(resultado.getCuestionarioOrigen()).isEqualTo(cuestionario);
        verify(cuestionarioCompartidoRepository).save(any(CuestionarioCompartido.class));
    }

    @Test
    @DisplayName("Aceptar compartido clona el cuestionario y actualiza el estado a ACEPTADO")
    void aceptarCompartido_exitoso() throws QuizlyException {
        CuestionarioCompartido compartido = new CuestionarioCompartido();
        compartido.setId(100L);
        compartido.setEmisor(emisor);
        compartido.setReceptor(receptor);
        compartido.setCuestionarioOrigen(cuestionario);
        compartido.setEstado(EstadoComparticion.PENDIENTE);

        Cuestionario clonado = new Cuestionario();
        clonado.setId(20L);
        clonado.setTitulo(cuestionario.getTitulo());
        clonado.setUsuario(receptor);

        when(cuestionarioCompartidoRepository.findById(100L)).thenReturn(Optional.of(compartido));
        when(cuestionarioService.clonarCuestionarioParaUsuario(cuestionario, receptor)).thenReturn(clonado);

        Cuestionario resultado = compartidoService.aceptarCompartido(100L, 2L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(20L);
        assertThat(compartido.getEstado()).isEqualTo(EstadoComparticion.ACEPTADO);
        assertThat(compartido.getCuestionarioClonado()).isEqualTo(clonado);
        assertThat(compartido.getFechaRespuesta()).isNotNull();
        verify(cuestionarioCompartidoRepository).save(compartido);
    }

    @Test
    @DisplayName("Rechazar compartido actualiza el estado a RECHAZADO sin clonar")
    void rechazarCompartido_exitoso() throws QuizlyException {
        CuestionarioCompartido compartido = new CuestionarioCompartido();
        compartido.setId(100L);
        compartido.setEmisor(emisor);
        compartido.setReceptor(receptor);
        compartido.setCuestionarioOrigen(cuestionario);
        compartido.setEstado(EstadoComparticion.PENDIENTE);

        when(cuestionarioCompartidoRepository.findById(100L)).thenReturn(Optional.of(compartido));

        compartidoService.rechazarCompartido(100L, 2L);

        assertThat(compartido.getEstado()).isEqualTo(EstadoComparticion.RECHAZADO);
        assertThat(compartido.getCuestionarioClonado()).isNull();
        assertThat(compartido.getFechaRespuesta()).isNotNull();
        verify(cuestionarioService, never()).clonarCuestionarioParaUsuario(any(), any());
        verify(cuestionarioCompartidoRepository).save(compartido);
    }

    @Test
    @DisplayName("Aceptar o rechazar por un usuario distinto al receptor lanza QuizlyException")
    void responder_porUsuarioDistinto_lanzaExcepcion() {
        CuestionarioCompartido compartido = new CuestionarioCompartido();
        compartido.setId(100L);
        compartido.setReceptor(receptor);
        compartido.setEstado(EstadoComparticion.PENDIENTE);

        when(cuestionarioCompartidoRepository.findById(100L)).thenReturn(Optional.of(compartido));

        assertThatThrownBy(() -> compartidoService.aceptarCompartido(100L, 999L))
                .isInstanceOf(QuizlyException.class)
                .hasMessageContaining("No tienes permiso");

        assertThatThrownBy(() -> compartidoService.rechazarCompartido(100L, 999L))
                .isInstanceOf(QuizlyException.class)
                .hasMessageContaining("No tienes permiso");
    }
}

