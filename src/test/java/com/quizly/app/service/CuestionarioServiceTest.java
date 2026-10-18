package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quizly.app.exception.QuizNotFoundException;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.CuestionarioSerie;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.session.UserQuizSession;
import com.quizly.app.repository.CuestionarioRepository;

import com.quizly.app.model.entity.Dificultad;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.repository.*;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class CuestionarioServiceTest {
    @Mock
    private PreguntaRepository preguntaRepository;
    @Mock
    private TipoPreguntaRepository tipoPreguntaRepository;
    @Mock
    private RespuestaUsuarioService respuestaUsuarioService;
    @Mock
    private QuizSessionService quizSessionService;
    @Mock
    private IntentoCuestionarioService intentoCuestionarioService;
    @Mock
    private SlugService slugService;
    @Mock
    private IntentoCuestionarioEstadoRepository intentoCuestionarioEstadoRepository;
    @Mock
    private CuestionarioRepository cuestionarioRepository;
    @Mock
    private CuestionarioSerieRepository cuestionarioSerieRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private IntentoCuestionarioRepository intentoCuestionarioRepository;
    @Mock
    private OpcionRespuestaRepository opcionRespuestaRepository;
    @Mock
    private TemaRepository temaRepository;
    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private DificultadRepository dificultadRepository;

    @InjectMocks
    private CuestionarioService cuestionarioService;

    @Test
    void requireAccessibleQuizSessionAcceptsOwnAttemptOnAccessibleQuiz() {
        HttpSession httpSession = mock(HttpSession.class);
        UserQuizSession quizSession = new UserQuizSession();
        quizSession.setIntentoCuestionarioId(7L);
        Cuestionario cuestionario = createQuiz("propio", 42L);
        IntentoCuestionario intento = createAttempt(cuestionario, 42L);

        when(quizSessionService.getRequired(httpSession)).thenReturn(quizSession);
        when(intentoCuestionarioService.getIntentoActivo(7L)).thenReturn(intento);
        when(cuestionarioRepository.findLastAccessibleVersionBySlug("propio", 42L))
                .thenReturn(Optional.of(cuestionario));

        assertSame(quizSession, cuestionarioService.requireAccessibleQuizSession("propio", 42L, httpSession));
    }

    @Test
    void requireAccessibleQuizSessionRejectsAttemptOwnedByAnotherUser() {
        HttpSession httpSession = mock(HttpSession.class);
        UserQuizSession quizSession = new UserQuizSession();
        quizSession.setIntentoCuestionarioId(7L);
        IntentoCuestionario intento = createAttempt(createQuiz("comun", null), 99L);

        when(quizSessionService.getRequired(httpSession)).thenReturn(quizSession);
        when(intentoCuestionarioService.getIntentoActivo(7L)).thenReturn(intento);
        when(cuestionarioRepository.findLastAccessibleVersionBySlug("comun", 42L))
                .thenReturn(Optional.empty());

        assertThrows(QuizNotFoundException.class,
                () -> cuestionarioService.requireAccessibleQuizSession("comun", 42L, httpSession));
    }

    @Test
    void requireAccessibleQuizSessionRejectsMismatchedSlug() {
        HttpSession httpSession = mock(HttpSession.class);
        UserQuizSession quizSession = new UserQuizSession();
        quizSession.setIntentoCuestionarioId(7L);
        IntentoCuestionario intento = createAttempt(createQuiz("propio", 42L), 42L);

        when(quizSessionService.getRequired(httpSession)).thenReturn(quizSession);
        when(intentoCuestionarioService.getIntentoActivo(7L)).thenReturn(intento);
        when(cuestionarioRepository.findLastAccessibleVersionBySlug("otro", 42L))
                .thenReturn(Optional.empty());

        assertThrows(QuizNotFoundException.class,
                () -> cuestionarioService.requireAccessibleQuizSession("otro", 42L, httpSession));
    }

        @Test
        void requireAccessibleQuizSessionRejectsForeignAttemptVersionWhenLatestVersionIsAccessible() {
        HttpSession httpSession = mock(HttpSession.class);
        UserQuizSession quizSession = new UserQuizSession();
        quizSession.setIntentoCuestionarioId(7L);
        IntentoCuestionario intento = createAttempt(createQuiz("compartido", 99L), 42L);
        Cuestionario currentAccessibleVersion = createQuiz("compartido", null);

        when(quizSessionService.getRequired(httpSession)).thenReturn(quizSession);
        when(intentoCuestionarioService.getIntentoActivo(7L)).thenReturn(intento);
        when(cuestionarioRepository.findLastAccessibleVersionBySlug("compartido", 42L))
            .thenReturn(Optional.of(currentAccessibleVersion));

        assertThrows(QuizNotFoundException.class,
            () -> cuestionarioService.requireAccessibleQuizSession("compartido", 42L, httpSession));
    }

    @Test
    void navigateToQuestionUpdatesCurrentIndexWhenTargetIsValid() {
        HttpSession httpSession = mock(HttpSession.class);
        UserQuizSession quizSession = new UserQuizSession();
        quizSession.setPreguntas(java.util.List.of(
            new com.quizly.app.model.session.QuestionQuizRecord(1L, "P1", java.util.List.of()),
            new com.quizly.app.model.session.QuestionQuizRecord(2L, "P2", java.util.List.of()),
            new com.quizly.app.model.session.QuestionQuizRecord(3L, "P3", java.util.List.of())
        ));
        quizSession.setCurrentQuestionIndex(2);
        quizSession.setMaxReachedQuestionIndex(2);

        when(quizSessionService.getRequired(httpSession)).thenReturn(quizSession);

        cuestionarioService.navigateToQuestion(0, httpSession);

        org.junit.jupiter.api.Assertions.assertEquals(0, quizSession.getCurrentQuestionIndex());
        org.mockito.Mockito.verify(intentoCuestionarioService).actualizarFechaUltimaActividad(quizSession);
    }

    @Test
    void navigateToQuestionIgnoresTargetWhenExceedsMaxReached() {
        HttpSession httpSession = mock(HttpSession.class);
        UserQuizSession quizSession = new UserQuizSession();
        quizSession.setPreguntas(java.util.List.of(
            new com.quizly.app.model.session.QuestionQuizRecord(1L, "P1", java.util.List.of()),
            new com.quizly.app.model.session.QuestionQuizRecord(2L, "P2", java.util.List.of()),
            new com.quizly.app.model.session.QuestionQuizRecord(3L, "P3", java.util.List.of())
        ));
        quizSession.setCurrentQuestionIndex(0);
        quizSession.setMaxReachedQuestionIndex(0);

        when(quizSessionService.getRequired(httpSession)).thenReturn(quizSession);

        // Intenta saltar a la pregunta 2 (índice 2) cuando solo tiene desbloqueada la 0
        cuestionarioService.navigateToQuestion(2, httpSession);

        org.junit.jupiter.api.Assertions.assertEquals(0, quizSession.getCurrentQuestionIndex());
        org.mockito.Mockito.verify(intentoCuestionarioService, org.mockito.Mockito.never())
            .actualizarFechaUltimaActividad(quizSession);
    }

    @Test
    void createQuizWithoutTemaAndAsignaturaSucceedsForAdmin() {
        QuizForm form = new QuizForm();
        form.setTitulo("Quiz General");
        form.setDificultadId(1L);
        form.setTemaId(null);
        form.setAsignaturaId(null);
        form.setPreguntas(java.util.List.of());

        Dificultad dificultad = new Dificultad();
        dificultad.setId(1L);

        when(dificultadRepository.findById(1L)).thenReturn(Optional.of(dificultad));
        when(slugService.generateSlug("Quiz General")).thenReturn("quiz-general");
        when(cuestionarioSerieRepository.existsBySlug("quiz-general")).thenReturn(false);
        when(cuestionarioSerieRepository.save(org.mockito.ArgumentMatchers.any(CuestionarioSerie.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(cuestionarioRepository.save(org.mockito.ArgumentMatchers.any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.createQuiz(form);

        org.junit.jupiter.api.Assertions.assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertNull(result.getTema());
        org.junit.jupiter.api.Assertions.assertNull(result.getUsuario());
        org.junit.jupiter.api.Assertions.assertEquals("Quiz General", result.getTitulo());
    }

    @Test
    void createQuizWithoutTemaAndAsignaturaSucceedsForStudent() {
        QuizForm form = new QuizForm();
        form.setTitulo("Quiz de Estudiante");
        form.setDificultadId(2L);
        form.setTemaId(null);
        form.setAsignaturaId(null);
        form.setPreguntas(java.util.List.of());

        Usuario estudiante = new Usuario();
        estudiante.setId(10L);

        Dificultad dificultad = new Dificultad();
        dificultad.setId(2L);

        when(dificultadRepository.findById(2L)).thenReturn(Optional.of(dificultad));
        when(slugService.generateSlug("Quiz de Estudiante")).thenReturn("quiz-de-estudiante");
        when(cuestionarioSerieRepository.existsBySlug("quiz-de-estudiante")).thenReturn(false);
        when(cuestionarioSerieRepository.save(org.mockito.ArgumentMatchers.any(CuestionarioSerie.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(cuestionarioRepository.save(org.mockito.ArgumentMatchers.any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.createQuiz(form, estudiante);

        org.junit.jupiter.api.Assertions.assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertNull(result.getTema());
        org.junit.jupiter.api.Assertions.assertSame(estudiante, result.getUsuario());
    }

    @Test
    void createQuizWithMismatchedTemaAndAsignaturaThrowsException() {
        QuizForm form = new QuizForm();
        form.setTitulo("Quiz Incoherente");
        form.setDificultadId(1L);
        form.setTemaId(5L);
        form.setAsignaturaId(99L);

        Asignatura asignaturaReal = new Asignatura();
        asignaturaReal.setId(1L);

        Tema tema = new Tema();
        tema.setId(5L);
        tema.setAsignatura(asignaturaReal);

        Asignatura asignaturaForm = new Asignatura();
        asignaturaForm.setId(99L);

        when(temaRepository.findById(5L)).thenReturn(Optional.of(tema));
        when(asignaturaRepository.findById(99L)).thenReturn(Optional.of(asignaturaForm));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> cuestionarioService.createQuiz(form));
    }

    @Test
    void updateQuizWithoutTemaAndAsignaturaSucceeds() {
        Cuestionario actual = createQuiz("quiz-general", null);
        actual.setId(1L);
        actual.setVersion(1);

        QuizForm form = new QuizForm();
        form.setTitulo("Quiz Actualizado");
        form.setDificultadId(1L);
        form.setTemaId(null);
        form.setAsignaturaId(null);
        form.setPreguntas(java.util.List.of());

        Dificultad dificultad = new Dificultad();
        dificultad.setId(1L);

        when(cuestionarioRepository.findById(1L)).thenReturn(Optional.of(actual));
        when(cuestionarioRepository.existsBySlugAndVersionGreaterThan("quiz-general", 1)).thenReturn(false);
        when(dificultadRepository.findById(1L)).thenReturn(Optional.of(dificultad));
        when(cuestionarioRepository.save(org.mockito.ArgumentMatchers.any(Cuestionario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Cuestionario result = cuestionarioService.updateQuiz(1L, form);

        org.junit.jupiter.api.Assertions.assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertNull(result.getTema());
        org.junit.jupiter.api.Assertions.assertEquals(2, result.getVersion());
        org.junit.jupiter.api.Assertions.assertEquals("Quiz Actualizado", result.getTitulo());
    }

    private static Cuestionario createQuiz(String slug, Long ownerId) {
        CuestionarioSerie serie = new CuestionarioSerie();
        serie.setSlug(slug);

        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setSerie(serie);
        if (ownerId != null) {
            Usuario owner = new Usuario();
            owner.setId(ownerId);
            cuestionario.setUsuario(owner);
        }
        return cuestionario;
    }

    private static IntentoCuestionario createAttempt(Cuestionario cuestionario, Long ownerId) {
        Usuario owner = new Usuario();
        owner.setId(ownerId);

        IntentoCuestionario intento = new IntentoCuestionario();
        intento.setCuestionario(cuestionario);
        intento.setUsuario(owner);
        return intento;
    }
}