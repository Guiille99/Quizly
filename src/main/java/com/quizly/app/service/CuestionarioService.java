package com.quizly.app.service;


import com.quizly.app.repository.IntentoCuestionarioEstadoRepository;
import com.quizly.app.repository.PreguntaRepository;
import com.quizly.app.repository.TipoPreguntaRepository;
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
import org.springframework.util.CollectionUtils;

import com.quizly.app.dto.form.OpcionRespuestaForm;
import com.quizly.app.dto.form.PreguntaForm;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.exception.QuizNotResumableException;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.logging.LogExecution;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Dificultad;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.OpcionRespuesta;
import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.entity.TipoPregunta;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.model.enums.TipoPreguntaEnum;
import com.quizly.app.model.session.OptionQuizRecord;
import com.quizly.app.model.session.QuestionQuizRecord;
import com.quizly.app.model.session.UserQuizResponse;
import com.quizly.app.model.session.UserQuizSession;
import com.quizly.app.model.view.quiz.QuestionSummary;
import com.quizly.app.model.view.quiz.QuizSummary;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.DificultadRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.repository.OpcionRespuestaRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.repository.UsuarioRepository;
import com.quizly.app.security.QuizlyUserDetails;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuestionarioService {
    private final PreguntaRepository preguntaRepository;
    private final TipoPreguntaRepository tipoPreguntaRepository;
    private final RespuestaUsuarioService respuestaUsuarioService;
    private final QuizSessionService quizSessionService;
    private final IntentoCuestionarioService intentoCuestionarioService;
    private final SlugService slugService;

    private final IntentoCuestionarioEstadoRepository intentoCuestionarioEstadoRepository;
    private final CuestionarioRepository cuestionarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;
    private final OpcionRespuestaRepository opcionRespuestaRepository;
    private final TemaRepository temaRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final DificultadRepository dificultadRepository;

    private Map<String, TipoPregunta> tiposPreguntaCache;

    @PostConstruct
    public void init(){
        List<TipoPregunta> tiposPreguntaList = tipoPreguntaRepository.findAll();
        this.tiposPreguntaCache = tiposPreguntaList.stream()
            .collect(Collectors.toMap(TipoPregunta::getCodigo, tipo -> tipo));
    }

    @Transactional
    public Cuestionario createQuiz(QuizForm form){
        return createQuiz(form, null);
    }

    @Transactional
    public Cuestionario createQuiz(QuizForm form, Usuario usuario){
        Tema tema = null;
        if (form.getTemaId() != null) {
            tema = temaRepository.findById(form.getTemaId())
                .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el tema seleccionado."));

            if (form.getAsignaturaId() != null) {
                Asignatura asignatura = asignaturaRepository.findById(form.getAsignaturaId())
                    .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado la asignatura seleccionada"));
                validarTemaDeAsignatura(tema, asignatura);
            }
        }

        Dificultad dificultad = dificultadRepository.findById(form.getDificultadId())
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado la dificultad seleccionada."));

        Cuestionario cuestionario = new Cuestionario();

        cuestionario.setTitulo(form.getTitulo());
        cuestionario.setDescripcion(form.getDescripcion());
        cuestionario.setTema(tema);
        cuestionario.setDificultad(dificultad);
        cuestionario.setUsuario(usuario);
        cuestionario.setSlug(resolveUniqueSlug(form.getTitulo()));
        cuestionario.setVersion(1);
        cuestionario.setTiempoLimite(calculateTiempoLimite(form));
        cuestionario.setPreguntasAleatorias(form.isPreguntasAleatorias());

        cuestionario = cuestionarioRepository.save(cuestionario);
        crearPreguntas(cuestionario, form.getPreguntas());

        return cuestionario;
    }

    @Transactional
    public Cuestionario updateQuiz(Long id, QuizForm form){
        Cuestionario cuestionarioActual = cuestionarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se ha encontrado la versión actual del cuestionario a actualizar."
            ));

        if (!isLastVersion(cuestionarioActual)) {
            throw new IllegalArgumentException(
                "Esta versión del cuestionario ya no puede modificarse porque existe una versión posterior."
            );
        }

        Tema tema = null;
        if (form.getTemaId() != null) {
            tema = temaRepository.findById(form.getTemaId())
                .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el tema seleccionado."));
        }

        Dificultad dificultad = dificultadRepository.findById(form.getDificultadId())
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado la dificultad seleccionada."));

        Cuestionario nuevaVersCuestionario = new Cuestionario();

        nuevaVersCuestionario.setTitulo(form.getTitulo());
        nuevaVersCuestionario.setDescripcion(form.getDescripcion());
        nuevaVersCuestionario.setTema(tema);
        nuevaVersCuestionario.setDificultad(dificultad);
        nuevaVersCuestionario.setUsuario(cuestionarioActual.getUsuario());

        nuevaVersCuestionario.setSlug(cuestionarioActual.getSlug());
        nuevaVersCuestionario.setVersion(cuestionarioActual.getVersion() + 1);

        nuevaVersCuestionario.setTiempoLimite(calculateTiempoLimite(form));
        nuevaVersCuestionario.setPreguntasAleatorias(form.isPreguntasAleatorias());

        nuevaVersCuestionario = cuestionarioRepository.save(nuevaVersCuestionario);

        crearPreguntas(nuevaVersCuestionario, form.getPreguntas());

        return nuevaVersCuestionario;
    }

    @Transactional
    public void deleteQuizVersion(Long id) throws QuizlyException{
        Cuestionario cuestionario = cuestionarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado el cuestionario solicitado"));

        if (!isLastVersion(cuestionario)) {
            throw new QuizlyException("Solo se puede eliminar la última versión del cuestionario.");
        }

        if (intentoCuestionarioRepository.existsByCuestionarioId(cuestionario.getId())) {
            throw new QuizlyException("No se puede eliminar esta versión porque tiene intentos asociados.");
        }

        // Eliminamos las opciones de respuesta
        opcionRespuestaRepository.deleteByCuestionarioId(id);

        // Eliminamos las preguntas asociadas
        preguntaRepository.deleteByCuestionarioId(id);
        
        // Eliminamos el cuestionario
        cuestionarioRepository.delete(cuestionario);
    }

    /**
     * Crea las preguntas añadidas desde el formulario
     * para el cuestionario pasado por parámetro.
     * 
     * @param cuestionario
     * @param preguntasForm
     */
    private void crearPreguntas(Cuestionario cuestionario, List<PreguntaForm> preguntasForm){
        if (CollectionUtils.isEmpty(preguntasForm)) {
            return;
        }

        for (PreguntaForm preguntaForm : preguntasForm) {
            validarPregunta(preguntaForm);
            
            Pregunta preguntaToPersist = new Pregunta();

            preguntaToPersist.setCuestionario(cuestionario);
            preguntaToPersist.setEnunciado(preguntaForm.getEnunciado());
            preguntaToPersist.setTipoPregunta(this.tiposPreguntaCache.get(preguntaForm.getTipoPreguntaCodigo()));
            preguntaToPersist.setOpcionesRespuesta(toOpcionesRespuesta(preguntaToPersist, preguntaForm.getOpcionesRespuesta()));
            
            // Persistimos la pregunta
            preguntaRepository.save(preguntaToPersist);
        }
    }

    private void validarPregunta(PreguntaForm preguntaForm){
        String tipoCodigo = preguntaForm.getTipoPreguntaCodigo();

        if (!this.tiposPreguntaCache.containsKey(tipoCodigo)) {
            throw new IllegalArgumentException(
                "El tipo de pregunta seleccionado no es válido."
            );
        }

        int numeroOpciones = preguntaForm.getOpcionesRespuesta().size();

        long numeroCorrectas = preguntaForm.getOpcionesRespuesta().stream()
            .filter(OpcionRespuestaForm::isCorrecta)
            .count();
        
        switch (tipoCodigo) {
            case "SINGLE_CHOICE" -> {
                if (numeroOpciones < 2 || numeroOpciones > 4) {
                    throw new IllegalArgumentException("Las preguntas de respuesta única deben tener entre 2 y 4 opciones");
                }
            }        
            
            case "TRUE_FALSE" -> {
                if (numeroOpciones != 2) {
                    throw new IllegalArgumentException("Las preguntas de verdadero/falso deben tener exactamente 2 opciones");
                }
            }
        }

        if (numeroCorrectas != 1) {
            throw new IllegalArgumentException("La pregunta debe tener exactamente una respuesta correcta");
        }
    }

    private List<OpcionRespuesta> toOpcionesRespuesta(Pregunta pregunta, List<OpcionRespuestaForm> opcionesRespuestaForm){
        List<OpcionRespuesta> opcionesRespuesta = new ArrayList<>();

        if (CollectionUtils.isEmpty(opcionesRespuestaForm)) {
            return opcionesRespuesta;
        }

        for (OpcionRespuestaForm opcionRespuestaForm : opcionesRespuestaForm) {
            OpcionRespuesta opcionRespuesta = new OpcionRespuesta();
            
            opcionRespuesta.setPregunta(pregunta);
            opcionRespuesta.setTexto(opcionRespuestaForm.getTexto());
            opcionRespuesta.setCorrecta(opcionRespuestaForm.isCorrecta());
            opcionRespuesta.setRetroalimentacion(opcionRespuestaForm.getRetroalimentacion());

            opcionesRespuesta.add(opcionRespuesta);
        }

        return opcionesRespuesta;
    }

    private Integer calculateTiempoLimite(QuizForm form) {
        if (form.isSinLimiteTiempo() || form.getTiempoLimite() == null) {
            return null;
        }

        return switch (form.getUnidadTiempo()) {
            case SEGUNDOS -> form.getTiempoLimite();
            case MINUTOS -> Math.multiplyExact(form.getTiempoLimite(), 60);
            case HORAS -> Math.multiplyExact(form.getTiempoLimite(), 3600);
        };
    }

    private void validarTemaDeAsignatura(Tema tema, Asignatura asignatura) {
        if (!tema.getAsignatura().getId().equals(asignatura.getId())) {
            throw new IllegalArgumentException("El tema seleccionado no pertenece a la asignatura indicada.");
        }
    }

    private String resolveUniqueSlug(String titulo) {
        String baseSlug = slugService.generateSlug(titulo);
        if (!cuestionarioRepository.existsBySlug(baseSlug)) {
            return baseSlug;
        }

        int suffix = 2;
        String candidate;
        do {
            candidate = baseSlug + "-" + suffix++;
        } while (cuestionarioRepository.existsBySlug(candidate));

        return candidate;
    }


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

    /**
     * Determina si la instancia del cuestionario pasada por parámetro
     * es la última versión
     * 
     * @param cuestionario
     * @return
     */
    public boolean isLastVersion(Cuestionario cuestionario){
        return !cuestionarioRepository.existsBySlugAndVersionGreaterThan(cuestionario.getSlug(), cuestionario.getVersion());
    }
}
