package com.quizly.app.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quizly.app.model.view.tarjeta.FalloPregunta;
import com.quizly.app.service.MazoService;

@ExtendWith(MockitoExtension.class)
class IntentoFinalizadoListenerTest {

    @Mock
    private MazoService mazoService;

    @InjectMocks
    private IntentoFinalizadoListener listener;

    private final IntentoFinalizadoEvent evento =
            new IntentoFinalizadoEvent(7L, List.of(new FalloPregunta(1L, "a", "b")));

    @Test
    @DisplayName("Delega los fallos en MazoService")
    void delega() {
        listener.alFinalizarIntento(evento);

        verify(mazoService).registrarFallos(7L, evento.fallos());
    }

    @Test
    @DisplayName("Una excepción al registrar los fallos no se propaga al flujo del cuestionario")
    void noPropagaExcepciones() {
        doThrow(new IllegalStateException("BD caída")).when(mazoService).registrarFallos(7L, evento.fallos());

        assertDoesNotThrow(() -> listener.alFinalizarIntento(evento));
    }
}
