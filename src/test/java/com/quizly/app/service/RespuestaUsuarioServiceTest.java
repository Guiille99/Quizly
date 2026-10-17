package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.OpcionRespuesta;
import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.model.entity.RespuestaUsuario;
import com.quizly.app.repository.RespuestaUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RespuestaUsuarioServiceTest {

    @Mock
    private RespuestaUsuarioRepository respuestaUsuarioRepository;

    @InjectMocks
    private RespuestaUsuarioService respuestaUsuarioService;

    @Test
    @DisplayName("guardarRespuesta crea una nueva entidad si no existía respuesta previa")
    void guardarRespuestaCreatesNewWhenNotPresent() {
        IntentoCuestionario intento = new IntentoCuestionario();
        intento.setId(10L);

        Pregunta pregunta = new Pregunta();
        pregunta.setId(20L);

        OpcionRespuesta opcion = new OpcionRespuesta();
        opcion.setId(30L);
        opcion.setCorrecta(true);

        when(respuestaUsuarioRepository.findByIntentoIdAndPreguntaId(10L, 20L)).thenReturn(Optional.empty());

        respuestaUsuarioService.guardarRespuesta(intento, pregunta, opcion);

        ArgumentCaptor<RespuestaUsuario> captor = ArgumentCaptor.forClass(RespuestaUsuario.class);
        verify(respuestaUsuarioRepository).save(captor.capture());

        RespuestaUsuario guardada = captor.getValue();
        assertSame(intento, guardada.getIntento());
        assertSame(pregunta, guardada.getPregunta());
        assertSame(opcion, guardada.getOpcionSeleccionada());
        assertTrue(guardada.isCorrecta());
    }

    @Test
    @DisplayName("guardarRespuesta actualiza la entidad existente sin crear duplicados")
    void guardarRespuestaUpdatesExistingWhenPresent() {
        IntentoCuestionario intento = new IntentoCuestionario();
        intento.setId(10L);

        Pregunta pregunta = new Pregunta();
        pregunta.setId(20L);

        OpcionRespuesta opcionAntigua = new OpcionRespuesta();
        opcionAntigua.setId(30L);
        opcionAntigua.setCorrecta(false);

        RespuestaUsuario existente = new RespuestaUsuario();
        existente.setId(99L);
        existente.setIntento(intento);
        existente.setPregunta(pregunta);
        existente.setOpcionSeleccionada(opcionAntigua);
        existente.setCorrecta(false);

        OpcionRespuesta opcionNueva = new OpcionRespuesta();
        opcionNueva.setId(31L);
        opcionNueva.setCorrecta(true);

        when(respuestaUsuarioRepository.findByIntentoIdAndPreguntaId(10L, 20L)).thenReturn(Optional.of(existente));

        respuestaUsuarioService.guardarRespuesta(intento, pregunta, opcionNueva);

        verify(respuestaUsuarioRepository).save(existente);
        assertEquals(99L, existente.getId());
        assertSame(opcionNueva, existente.getOpcionSeleccionada());
        assertTrue(existente.isCorrecta());
    }
}

