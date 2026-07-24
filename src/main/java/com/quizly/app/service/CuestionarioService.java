package com.quizly.app.service;


import com.quizly.app.repository.IntentoCuestionarioEstadoRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.quizly.app.exception.QuizNotResumableException;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.logging.LogExecution;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.IntentoCuestionarioEstado;
import com.quizly.app.model.entity.OpcionRespuesta;
import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.model.session.OptionQuizRecord;
import com.quizly.app.model.session.QuestionQuizRecord;
import com.quizly.app.model.session.UserQuizResponse;
import com.quizly.app.model.session.UserQuizSession;
import com.quizly.app.model.view.quiz.QuestionSummary;
import com.quizly.app.model.view.quiz.QuizSummary;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.repository.OpcionRespuestaRepository;
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.security.QuizlyUserDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuestionarioService {
    private final RespuestaUsuarioService respuestaUsuarioService;
    private final IntentoCuestionarioEstadoRepository intentoCuestionarioEstadoRepository;
    private final QuizSessionService quizSessionService;
    private final CuestionarioRepository cuestionarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final IntentoCuestionarioService intentoCuestionarioService;
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;
    private final OpcionRespuestaRepository opcionRespuestaRepository;

    public Page<Cuestionario> getCuestionariosPaginated(Pageable pageable){
       return cuestionarioRepository.findAllByOrderByTemaNombreAsc(pageable);
    }

    public Page<Cuestionario> getCuestionariosRecientesPaginated(Pageable pageable, QuizlyUserDetails authUser){
        List<String> intentosCuestionarioEstados = List.of(
            IntentoCuestionarioEstadoEnum.FINISHED.toString(), 
            IntentoCuestionarioEstadoEnum.ABANDONNED.toString()
        );

        return cuestionarioRepository.getCuestionariosRecientesByUsuarioYEstado(authUser.getId(), intentosCuestionarioEstados, pageable);
    }

    public Optional<Cuestionario> getLastVersionQuizBySlug(String slug){
        return cuestionarioRepository.findLastVersionBySlug(slug);
    }

    @LogExecution
    @Transactional
    public void startQuiz(String slug, Long userId, HttpSession session){
        
        quizSessionService.clear(session);
        Cuestionario cuestionario = cuestionarioRepository.findLastVersionBySlug(slug)
            .orElseThrow(() -> new EntityNotFoundException("No existe el cuestionario."));
        
        // Obtenemos el intento abierto del usuario para este cuestionario
        Optional<IntentoCuestionario> intentoAbierto = intentoCuestionarioRepository.findIntentoAbiertoByUsuarioYSlug(
            userId, cuestionario.getSlug());

        
        if (intentoAbierto.isPresent()) {
            // Marcar como abandonado el intento abierto antes de comenzar uno nuevo de este mismo cuestionario
            intentoCuestionarioService.abandonarIntento(intentoAbierto.get());
        }

       
        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("No existe el usuario."));

        IntentoCuestionario intentoCuestionario = intentoCuestionarioService.crearIntentoCuestionario(cuestionario, usuario);
        quizSessionService.create(session, intentoCuestionario);
        
        log.info("El usuario '{}' inició el cuestionario '{}' (id={})", usuario.getUsername(), cuestionario.getTitulo(), cuestionario.getId());
    }

    /**
     * Reanuda un cuestionario previamente inicido por el usuario.
     * @param slug
     * @param userId
     * @param session
     * @throws QuizNotResumableException 
     */
    @LogExecution
    @Transactional
    public void resumeQuiz(String slug, Long userId, HttpSession session) throws QuizNotResumableException{
        // Limpiamos cualquier sesión anterior
        quizSessionService.clear(session);

        IntentoCuestionario intento = intentoCuestionarioService
            .getIntentoReanudable(userId, slug)
            .orElseThrow(() -> new QuizNotResumableException("El intento ya no puede reanudarse."));

        quizSessionService.resume(session, intento);

        intento.actualizarUltimaActividad();
        intentoCuestionarioRepository.save(intento);

        log.info("El usuario '{}' reanudó el cuestionario '{}' (intento={})",
            intento.getUsuario().getUsername(),
            intento.getCuestionario().getId(),
            intento.getId()
        );
    }

    @LogExecution
    @Transactional
    public IntentoCuestionario finishQuiz(HttpSession httpSession){
        UserQuizSession session = quizSessionService.getRequired(httpSession);

        IntentoCuestionario intentoCuestionario = intentoCuestionarioRepository.findById(session.getIntentoCuestionarioId()).get();

        // Si el intento ya cuenta con una fecha de finalización
        // noo hacemos nada
        if (intentoCuestionario.getFechaFin() != null) {
            return null;
        }

        int aciertos = session.getScore();
        int fallos = session.getPreguntas().size() - aciertos;

        LocalDateTime fechaFin = LocalDateTime.now();

        long tiempoEmpleado = Duration.between(intentoCuestionario.getFechaInicio(), fechaFin).toSeconds();

        intentoCuestionario.setFechaFin(fechaFin);
        intentoCuestionario.setAciertos(aciertos);
        intentoCuestionario.setFallos(fallos);
        intentoCuestionario.setTiempoEmpleado(tiempoEmpleado);
        intentoCuestionario.setEstado(intentoCuestionarioEstadoRepository.findByCodigo(IntentoCuestionarioEstadoEnum.FINISHED.toString()).get());

        // Persistimos el intento del cuestionario
        IntentoCuestionario intentoCuestionarioPersist = intentoCuestionarioRepository.save(intentoCuestionario);

        // Actualizamos el index de la pregunta actual a la última pregunta 
        // para que se entienda que ha llegado al final
        session.setCurrentQuestionIndex(session.getPreguntas().size());

        log.info("El usuario '{}' finalizó el cuestionario '{}' con un {}% ({} aciertos, {} fallos, {} s)",
            intentoCuestionarioPersist.getUsuario().getUsername(),
            intentoCuestionarioPersist.getCuestionario().getTitulo(),
            intentoCuestionarioPersist.getPorcentajeAciertosRedondeado(),
            intentoCuestionarioPersist.getAciertos(),
            intentoCuestionarioPersist.getFallos(),
            intentoCuestionarioPersist.getTiempoEmpleado()
        );
        return intentoCuestionarioPersist;
    }

    @Transactional
    public void answerQuestion(Long optionId, HttpSession httpSession){
        UserQuizSession quizSession = quizSessionService.getRequired(httpSession);

        QuestionQuizRecord question = quizSession.getCurrentQuestion();

        OptionQuizRecord selected = question.options().stream()
            .filter(option -> option.id().equals(optionId))
            .findFirst()
            .get();

        boolean correct = selected.correct();

        quizSession.addResponse(new UserQuizResponse(
            question.id(),
            selected.id(),
            selected.text(),
            correct
        ));

        if (correct) {
            quizSession.increaseScore();
        }

        IntentoCuestionario intento = intentoCuestionarioService.getIntentoActivo(quizSession.getIntentoCuestionarioId());
        OpcionRespuesta opcion = opcionRespuestaRepository.findById(optionId).get();

        // Persistimos la respuesta
        respuestaUsuarioService.guardarRespuesta(intento, opcion.getPregunta(), opcion);

        quizSession.nextQuestion();

        intentoCuestionarioService.actualizarFechaUltimaActividad(quizSession);

    }

    public QuizSummary buildSummary(UserQuizSession session, IntentoCuestionario intentoCuestionario){
        List<QuestionSummary> questions = new ArrayList<>();

        // Índice de preguntas por ID
        Map<Long, QuestionQuizRecord> questionMap = session.getPreguntas().stream()
            .collect(Collectors.toMap(QuestionQuizRecord::id, Function.identity()));


        for (UserQuizResponse response : session.getResponses()) {
            QuestionQuizRecord question = questionMap.get(response.preguntaId());

            OptionQuizRecord selectedOption = question.options().stream()
                .filter(o -> o.id().equals(response.opcionSeleccionadaId()))
                .findFirst()
                .orElseThrow();

            OptionQuizRecord correctOption = question.options().stream()
                .filter(OptionQuizRecord::correct)
                .findFirst()
                .orElseThrow();

            questions.add(new QuestionSummary(
                question.statement(),
                response.correcta(),
                selectedOption.text(),
                correctOption.text(),
                correctOption.explanation()
            ));
        }

        int total = session.getPreguntas().size();

        return new QuizSummary(session.getTitulo(), session.getTema(), session.getDificultad(), session.getSlug(), 
            total, intentoCuestionario.getAciertos(), intentoCuestionario.getFallos(), intentoCuestionario.getPorcentajeAciertosRedondeado(), 
            intentoCuestionario.getTiempoEmpleadoFormateado(), session.isTiempoAgotado(), questions);
    }

    public QuizSummary getSummary (UserQuizSession session){
        IntentoCuestionario intentoCuestionario = intentoCuestionarioRepository.findById(session.getIntentoCuestionarioId()).get();

        return buildSummary(session, intentoCuestionario);
    }

    public Page<Cuestionario> getCuestionariosPaginatedByTema(Tema tema, String titleFilter, Pageable pageable){
        
        if (tema == null) {
            return Page.empty();
        }

        // Gestionar la ordenación
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("titulo").ascending();
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Cuestionario> cuestionariosPage;

        if (StringUtils.isNotBlank(titleFilter)) {
            cuestionariosPage = cuestionarioRepository.findByTituloContainingIgnoreCaseAndTemaId(titleFilter, tema.getId(), pageable);
        } else{
            cuestionariosPage = cuestionarioRepository.findByTemaId(tema.getId(), pageable);
        }

        return new PageImpl<>(cuestionariosPage.getContent(), sortedPageable, cuestionariosPage.getTotalElements());
    }
}
