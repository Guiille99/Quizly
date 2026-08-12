package com.quizly.app.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.quizly.app.exception.UserQuizSessionNotFoundException;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.model.entity.RespuestaUsuario;
import com.quizly.app.model.session.OptionQuizRecord;
import com.quizly.app.model.session.QuestionQuizRecord;
import com.quizly.app.model.session.UserQuizResponse;
import com.quizly.app.model.session.UserQuizSession;

import jakarta.servlet.http.HttpSession;

@Service
public class QuizSessionService {
    public static final String SESSION_KEY = "QUIZ_SESSION";

    /**
     * Crea una nueva sesión de cuestionario.
     * @param httpSession
     * @param intentoCuestionario
     * @return
     */
    public UserQuizSession create(HttpSession httpSession, IntentoCuestionario intentoCuestionario){
        UserQuizSession session = new UserQuizSession();

        List<Pregunta> questionsAux = new ArrayList<>(intentoCuestionario.getCuestionario().getPreguntas());

        if (intentoCuestionario.getCuestionario().isPreguntasAleatorias()) {
            Collections.shuffle(questionsAux);
        }

        List<QuestionQuizRecord> questions = questionsAux
            .stream()
            .map(this::toQuestionQuizSnapshot)
            .toList();

        session.setIntentoCuestionarioId(intentoCuestionario.getId());
        session.setCuestionarioId(intentoCuestionario.getCuestionario().getId());
        session.setTitulo(intentoCuestionario.getCuestionario().getTitulo());
        session.setTema(intentoCuestionario.getCuestionario().getTema().getNombre());
        session.setDificultad(intentoCuestionario.getCuestionario().getDificultad().getNombre());
        session.setSlug(intentoCuestionario.getCuestionario().getSlug());
        session.setPreguntas(questions);
        session.setCurrentQuestionIndex(0);
        session.setScore(0);
        session.setStartedAt(Instant.now());
        session.setResponses(new ArrayList<>());
        session.setFechaExpiracion(intentoCuestionario.getFechaExpiracion());
        session.setFechaUltimaActividad(intentoCuestionario.getFechaUltimaActividad());

        httpSession.setAttribute(SESSION_KEY, session);

        return session;
    }

    /**
     * Reconstruye una sesión existente a partir de un intento en progreso.
     * @param httpSession
     * @param intentoCuestionario
     * @return
     */
    public UserQuizSession resume(HttpSession httpSession, IntentoCuestionario intentoCuestionario){
        UserQuizSession session = new UserQuizSession();

        List<QuestionQuizRecord> questions = intentoCuestionario.getCuestionario()
            .getPreguntas()
            .stream()
            .map(this::toQuestionQuizSnapshot)
            .toList();

        List<UserQuizResponse> responses = new ArrayList<>();

        int score = 0;

        if (intentoCuestionario.getRespuestas() != null) {
            for (RespuestaUsuario respuesta : intentoCuestionario.getRespuestas()) {
                boolean correcta = respuesta.getOpcionSeleccionada().getCorrecta();

                responses.add(new UserQuizResponse(
                    respuesta.getPregunta().getId(),
                    respuesta.getOpcionSeleccionada().getId(),
                    respuesta.getOpcionSeleccionada().getTexto(),
                    correcta
                ));

                if (correcta) {
                    score++;
                }
            }
        }

        session.setIntentoCuestionarioId(intentoCuestionario.getId());
        session.setCuestionarioId(intentoCuestionario.getCuestionario().getId());
        session.setTitulo(intentoCuestionario.getCuestionario().getTitulo());
        session.setTema(intentoCuestionario.getCuestionario().getTema().getNombre());
        session.setDificultad(intentoCuestionario.getCuestionario().getDificultad().getNombre());
        session.setSlug(intentoCuestionario.getCuestionario().getSlug());
        session.setPreguntas(questions);

        // La siguiente pregunta será la inmediatamente posterior a las ya respondidas
        session.setCurrentQuestionIndex(responses.size());

        session.setScore(score);

        // Conservamos la fecha original de inicio
        session.setStartedAt(intentoCuestionario.getFechaInicio().atZone(ZoneId.systemDefault()).toInstant());

        session.setResponses(responses);
        session.setFechaExpiracion(intentoCuestionario.getFechaExpiracion());
        session.setFechaUltimaActividad(intentoCuestionario.getFechaUltimaActividad());

        httpSession.setAttribute(SESSION_KEY, session);
        return session;
    }

    private QuestionQuizRecord toQuestionQuizSnapshot(Pregunta pregunta){
        List<OptionQuizRecord> options = pregunta.getOpcionesRespuesta().stream()
            .map(opcion -> new OptionQuizRecord(opcion.getId(), opcion.getTexto(), opcion.getCorrecta(), opcion.getRetroalimentacion()))
            .toList();

        return new QuestionQuizRecord(pregunta.getId(), pregunta.getEnunciado(), options);
    }

    /**
     * Obtieen la sesión actual
     * @param httpSession
     * @return
     */
    public Optional<UserQuizSession> get(HttpSession httpSession){
        Object attribute = httpSession.getAttribute(SESSION_KEY);

        if (attribute instanceof UserQuizSession session) {
            return Optional.of(session);
        }

        return Optional.empty();
    }

    /**
     * Devuelve la sesión o lanza excepción
     * @param httpSession
     * @return
     */
    public UserQuizSession getRequired(HttpSession httpSession){
        return get(httpSession).orElseThrow(UserQuizSessionNotFoundException::new);
    }

    /**
     * Guarda la sesión.
     */
    public void save(HttpSession httpSession, UserQuizSession session) {
        httpSession.setAttribute(SESSION_KEY, session);
    }

    /**
     * Elimina la sesión.
     */
    public void clear(HttpSession httpSession) {
        httpSession.removeAttribute(SESSION_KEY);
    }

    /**
     * Añade una respuesta al historial.
     */
    public void addResponse(HttpSession httpSession, UserQuizResponse response) {

        UserQuizSession session = getRequired(httpSession);

        session.getResponses().add(response);

        save(httpSession, session);
    }

    /**
     * Incrementa la puntuación.
     */
    public void increaseScore(HttpSession httpSession) {

        UserQuizSession session = getRequired(httpSession);

        session.increaseScore();

        save(httpSession, session);
    }

    /**
     * Avanza a la siguiente pregunta.
     */
    public void nextQuestion(HttpSession httpSession) {

        UserQuizSession session = getRequired(httpSession);

        session.nextQuestion();

        save(httpSession, session);
    }

    /**
     * Indica si el cuestionario ha terminado.
     */
    public boolean isFinished(HttpSession httpSession, int totalQuestions) {

        UserQuizSession session = getRequired(httpSession);

        return session.getCurrentQuestionIndex() >= totalQuestions;
    }
}
