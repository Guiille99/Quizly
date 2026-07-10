package com.quizly.app.service;


import com.quizly.app.repository.IntentoCuestionarioEstadoRepository;
import com.quizly.app.repository.RespuestaUsuarioRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.IntentoCuestionarioEstado;
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
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.security.QuizlyUserDetails;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

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

    public Optional<Cuestionario> getCuestionarioBySlug(String slug){
        return cuestionarioRepository.getUltimaVersionCuestionarioBySlug(slug);
    }

    @Transactional
    public void startQuiz(String slug, Long userId, HttpSession session){
        
        quizSessionService.clear(session);
        Cuestionario cuestionario = cuestionarioRepository.getUltimaVersionCuestionarioBySlug(slug)
            .orElseThrow(() -> new EntityNotFoundException("No existe el cuestionario."));
        
        // Obtenemos todos los intentos abiertos del usuario para este cuestionario
        List<IntentoCuestionario> cuestionariosAbiertos = intentoCuestionarioRepository.findIntentosAbiertoByUsuarioYCuestionario(
            userId, cuestionario.getId());

        
        if (!CollectionUtils.isEmpty(cuestionariosAbiertos)) {
            // Marcar como abandonados esos intentos abiertos antes de comenzar uno nuevo de este mismo cuestionario
            List<Long> cuestionariosAbiertosId = cuestionariosAbiertos.stream()
                .map(i -> i.getCuestionario().getId())
                .toList();

            IntentoCuestionarioEstado estadoIntentoAbandonado = intentoCuestionarioEstadoRepository.
                findByCodigo(IntentoCuestionarioEstadoEnum.ABANDONNED.toString()).get();

            intentoCuestionarioRepository.marcarIntentosAbandonados(userId, cuestionariosAbiertosId, estadoIntentoAbandonado);
        }

       
        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("No existe el usuario."));



        // Cuestionario cuestionario = cuestionarioOpt.get();
        // Usuario usuario = usuarioOpt.get();

        // Optional<IntentoCuestionario> intentoAbierto = intentoCuestionarioRepository.findIntentoAbiertoByUsuarioYCuestionario(
        //     userId, cuestionario.getId());
        

        // if (intentoAbierto.isPresent()) {
        //     quizSessionService.create(session, intentoAbierto.get());
        //     return;
        // }

        IntentoCuestionario intentoCuestionario = intentoCuestionarioService.crearIntentoCuestionario(cuestionario, usuario);
        quizSessionService.create(session, intentoCuestionario);
        
    }

    @Transactional
    public IntentoCuestionario finishQuiz(HttpSession httpSession){
        UserQuizSession session = quizSessionService.getRequired(httpSession);

        IntentoCuestionario intentoCuestionario = intentoCuestionarioRepository.findById(session.getIntentoCuestionarioId()).get();

        int aciertos = session.getScore();
        int fallos = session.getPreguntas().size() - aciertos;

        LocalDateTime fechaFin = LocalDateTime.now();

        long tiempoEmpleado = Duration.between(intentoCuestionario.getFechaInicio(), fechaFin).toSeconds();

        intentoCuestionario.setFechaFin(fechaFin);
        intentoCuestionario.setAciertos(aciertos);
        intentoCuestionario.setFallos(fallos);
        intentoCuestionario.setTiempoEmpleado(tiempoEmpleado);
        intentoCuestionario.setEstado(intentoCuestionarioEstadoRepository.findByCodigo(IntentoCuestionarioEstadoEnum.FINISHED.toString()).get());

        // Persistimos el intento del cuestionario y las respuestas del usuario
        IntentoCuestionario intentoCuestionarioPersist = intentoCuestionarioRepository.save(intentoCuestionario);
        respuestaUsuarioService.saveAll(session.getResponses(), intentoCuestionario);

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

        quizSession.nextQuestion();

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
            intentoCuestionario.getTiempoEmpleadoFormateado(), questions);
    }

    public QuizSummary getSummary (UserQuizSession session){
        IntentoCuestionario intentoCuestionario = intentoCuestionarioRepository.findById(session.getIntentoCuestionarioId()).get();

        return buildSummary(session, intentoCuestionario);
    }
}
