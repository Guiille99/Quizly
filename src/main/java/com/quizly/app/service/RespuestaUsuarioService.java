package com.quizly.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.OpcionRespuesta;
import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.model.entity.RespuestaUsuario;
import com.quizly.app.model.session.UserQuizResponse;
import com.quizly.app.repository.RespuestaUsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RespuestaUsuarioService {
    private final RespuestaUsuarioRepository respuestaUsuarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveAll(List<UserQuizResponse> responses, IntentoCuestionario intentoCuestionario){
        List<RespuestaUsuario> respuestasUsuario = responses.stream()
            .map(response -> {
                RespuestaUsuario respuestaUsuario = new RespuestaUsuario();
                respuestaUsuario.setIntento(intentoCuestionario);
                respuestaUsuario.setCorrecta(response.correcta());
                respuestaUsuario.setPregunta(entityManager.getReference(Pregunta.class, response.preguntaId()));
                respuestaUsuario.setOpcionSeleccionada(entityManager.getReference(OpcionRespuesta.class, response.opcionSeleccionadaId()));

                return respuestaUsuario;
            })
            .toList();

            respuestaUsuarioRepository.saveAll(respuestasUsuario);
    }

    @Transactional
    public void guardarRespuesta(IntentoCuestionario intento, Pregunta pregunta, OpcionRespuesta opcionSeleccionada){
        RespuestaUsuario respuesta = new RespuestaUsuario();

        respuesta.setIntento(intento);
        respuesta.setPregunta(pregunta);
        respuesta.setOpcionSeleccionada(opcionSeleccionada);
        respuesta.setCorrecta(opcionSeleccionada.getCorrecta());

        respuestaUsuarioRepository.save(respuesta);
    }
}
