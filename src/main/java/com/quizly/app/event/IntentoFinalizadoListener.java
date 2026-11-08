package com.quizly.app.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.quizly.app.service.MazoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Alimenta el mazo de "Repaso de fallos" una vez confirmado el intento. Tras el commit para que un
 * rollback no deje tarjetas de un intento no terminado, y con captura total para que un fallo aquí
 * nunca afecte al usuario que ya ha terminado su cuestionario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentoFinalizadoListener {

    private final MazoService mazoService;

    // Sin fallbackExecution: finishQuiz es transaccional; si dejara de serlo el evento se descartaría
    // en silencio (lo cubre el test de finishQuiz con la anotación).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alFinalizarIntento(IntentoFinalizadoEvent evento) {
        try {
            mazoService.registrarFallos(evento.usuarioId(), evento.fallos());
        } catch (Exception e) {
            log.error("No se pudieron registrar las tarjetas de fallos del usuario {}", evento.usuarioId(), e);
        }
    }
}
