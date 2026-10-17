package com.quizly.app.controller;

import com.quizly.app.exception.QuizNotResumableException;
import com.quizly.app.exception.QuizNotFoundException;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.model.session.AnswerRequest;
import com.quizly.app.model.session.UserQuizSession;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.CuestionarioService;
import com.quizly.app.service.FavoritoService;
import com.quizly.app.service.IntentoCuestionarioService;
import com.quizly.app.service.QuizSessionService;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizly.app.util.Constantes;
import com.quizly.app.web.notification.ToastService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.quizly.app.repository.DificultadRepository;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/cuestionarios")
public class CuestionariosController {

    private final ToastService toastService;
    private final CuestionarioService cuestionarioService;
    private final FavoritoService favoritoService;
    private final QuizSessionService quizSessionService;
    private final IntentoCuestionarioService intentoCuestionarioService;
    private final DificultadRepository dificultadRepository;
    private final com.quizly.app.web.model.BreadcrumbContext breadcrumbContext;
    private final com.quizly.app.service.BreadcrumbService breadcrumbService;


    @GetMapping
    public String showTodosCuestionarios(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Model model, HttpServletRequest request, 
            @PageableDefault(size = 5) Pageable pageable) {
        
        Page<Cuestionario> cuestionarios = cuestionarioService.getCuestionariosPaginated(search, dificultadId, authUser.getId(), pageable);

        model.addAttribute("cuestionariosPage", cuestionarios);
        model.addAttribute("favoriteSerieIds", favoritoService.getFavoriteSerieIds(authUser.getId(), cuestionarios.getContent()));
        model.addAttribute("dificultades", dificultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("search", search);
        model.addAttribute("dificultadId", dificultadId);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.cuestionariosTodos());
        return "cuestionarios/cuestionarios-todos";
    }

    @GetMapping("/recientes")
    public String showCuestionariosRecientes(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            Model model, HttpServletRequest request,
            @AuthenticationPrincipal QuizlyUserDetails authUser, 
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<Cuestionario> cuestionariosRecientes = cuestionarioService.getCuestionariosRecientesPaginated(search, dificultadId, pageable, authUser);
        
        model.addAttribute("cuestionariosRecientesPage", cuestionariosRecientes);
        model.addAttribute("favoriteSerieIds", favoritoService.getFavoriteSerieIds(authUser.getId(), cuestionariosRecientes.getContent()));
        model.addAttribute("dificultades", dificultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("search", search);
        model.addAttribute("dificultadId", dificultadId);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        breadcrumbContext.set(breadcrumbService.cuestionariosRecientes());
        return "cuestionarios/cuestionarios-recientes";
    }
    
 
    
    @GetMapping("/table-data")
    public String getCuestionariosDatosTabla(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            @AuthenticationPrincipal QuizlyUserDetails authUser,
            Model model, 
            @PageableDefault(size = 5) Pageable pageable) {
        
        Page<Cuestionario> cuestionarios = cuestionarioService.getCuestionariosPaginated(search, dificultadId, authUser.getId(), pageable);

        model.addAttribute("cuestionariosPage", cuestionarios);
        model.addAttribute("favoriteSerieIds", favoritoService.getFavoriteSerieIds(authUser.getId(), cuestionarios.getContent()));
        model.addAttribute("dificultades", dificultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("search", search);
        model.addAttribute("dificultadId", dificultadId);

        return "cuestionarios/cuestionarios-todos :: lista-cuestionarios_fragment";
    }

    @GetMapping("/recientes/table-data")
    public String getCuestionariosRecientesDatosTabla(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dificultadId", required = false) Long dificultadId,
            Model model, @AuthenticationPrincipal QuizlyUserDetails authUser,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<Cuestionario> cuestionariosRecientes = cuestionarioService.getCuestionariosRecientesPaginated(search, dificultadId, pageable, authUser);

        model.addAttribute("cuestionariosRecientesPage", cuestionariosRecientes);
        model.addAttribute("favoriteSerieIds", favoritoService.getFavoriteSerieIds(authUser.getId(), cuestionariosRecientes.getContent()));
        model.addAttribute("dificultades", dificultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("search", search);
        model.addAttribute("dificultadId", dificultadId);

        return "cuestionarios/cuestionarios-recientes :: lista-cuestionarios_fragment";
    }
    

    @GetMapping("/{slug}")
    public String showStart(@PathVariable String slug, Model model, @AuthenticationPrincipal QuizlyUserDetails authUser) {
        Cuestionario cuestionario = cuestionarioService.getLastAccessibleVersionQuizBySlug(slug, authUser.getId())
            .orElseThrow(QuizNotFoundException::new);
        Optional<IntentoCuestionario> intento = intentoCuestionarioService.getIntentoReanudable(
            authUser.getId(), cuestionario.getSlug());

        model.addAttribute("quiz", cuestionario);
        model.addAttribute("isFavorite", favoritoService.isFavorite(authUser.getId(), slug));

        if (intento.isPresent()) {
            IntentoCuestionario intentoReanudable = intento.get();
            model.addAttribute("intentoReanudable", intentoReanudable);
            model.addAttribute("isPreviousVersion", !intentoReanudable.perteneceAVersion(cuestionario));
        }

        return "cuestionarios/quizStart";
    }

    @PostMapping("/{slug}/start")
    public String startQuiz(@PathVariable String slug, @AuthenticationPrincipal QuizlyUserDetails user, HttpSession session) {
        cuestionarioService.startQuiz(slug, user.getId(), session);

        return "redirect:/cuestionarios/" + slug + "/question";
    }
    
    
    @PostMapping("/{slug}/resume")
    public String resumeQuiz(@PathVariable String slug, @AuthenticationPrincipal QuizlyUserDetails user, 
            HttpSession session, RedirectAttributes ra) {
        try {
            cuestionarioService.resumeQuiz(slug, user.getId(), session);
        } catch (QuizNotResumableException e) {
            toastService.warning(ra, e.getMessage());
            return "redirect:/cuestionarios/" + slug;
        }

        return "redirect:/cuestionarios/" + slug + "/question";
    }
    
    
    @GetMapping("/{slug}/question")
    public String showQuizQuestion(@PathVariable String slug, @AuthenticationPrincipal QuizlyUserDetails user,
            HttpSession session, Model model, RedirectAttributes ra) {

        UserQuizSession quizSession = cuestionarioService.requireAccessibleQuizSession(slug, user.getId(), session);

        IntentoCuestionario intento = intentoCuestionarioService.getIntentoActivo(quizSession.getIntentoCuestionarioId());

        // Si el intento ya no está en progreso (ha sido abandonado automáticamente),
        // eliminamos la sesión y volvemos a la pantalla inicial
        if (!IntentoCuestionarioEstadoEnum.IN_PROGRESS.name().equals(intento.getEstado().getCodigo())) {
            quizSessionService.clear(session);
            toastService.warning(ra, "El intento ha finalizado. Deberá comenzar el cuestionario nuevamente.");
            return "redirect:/cuestionarios/" + slug;
        }

        if (quizSession.isFinished()) {
            return "redirect:/cuestionarios/" + slug + "/summary";
        }

        if (quizSession.isTiempoAgotado()) {
            cuestionarioService.finishQuiz(session);
            return "redirect:/cuestionarios/" + slug + "/summary";
        }

        model.addAttribute("quizSession", quizSession);
        model.addAttribute("question", quizSession.getCurrentQuestion());
        model.addAttribute("selectedOptionId", quizSession.getSelectedOptionIdForQuestion(quizSession.getCurrentQuestion().id()));

       return "cuestionarios/questionQuiz";
    }
    
    @PostMapping("/{slug}/answer")
    public String answerQuizQuestion(@PathVariable String slug, @ModelAttribute AnswerRequest request,
        @AuthenticationPrincipal QuizlyUserDetails user, HttpSession session, HttpServletResponse response, Model model) {

        cuestionarioService.requireAccessibleQuizSession(slug, user.getId(), session);
        cuestionarioService.answerQuestion(request.optionId(), session);
        UserQuizSession quiz = quizSessionService.getRequired(session);

        if (quiz.isFinished()) {
            cuestionarioService.finishQuiz(session);
            response.setHeader("HX-Redirect", "/cuestionarios/" + slug + "/summary");
            return null;
        }

        model.addAttribute("quizSession", quiz);
        model.addAttribute("question", quiz.getCurrentQuestion());
        model.addAttribute("selectedOptionId", quiz.getSelectedOptionIdForQuestion(quiz.getCurrentQuestion().id()));

        // Dispara el evento para sincronizar el contador con HTMX
        response.setHeader("HX-Trigger",
            "{\"quizTimerSync\":{\"seconds\":"
            + quiz.getSegundosRestantes()
            + "}}");

        return "cuestionarios/questionQuiz :: quiz-content";
    }

    @PostMapping("/{slug}/navigate")
    public String navigateQuizQuestion(@PathVariable String slug, @RequestParam("targetIndex") int targetIndex,
        @AuthenticationPrincipal QuizlyUserDetails user, HttpSession session, HttpServletResponse response, Model model) {

        cuestionarioService.requireAccessibleQuizSession(slug, user.getId(), session);
        cuestionarioService.navigateToQuestion(targetIndex, session);
        UserQuizSession quiz = quizSessionService.getRequired(session);

        model.addAttribute("quizSession", quiz);
        model.addAttribute("question", quiz.getCurrentQuestion());
        model.addAttribute("selectedOptionId", quiz.getSelectedOptionIdForQuestion(quiz.getCurrentQuestion().id()));

        // Dispara el evento para sincronizar el contador con HTMX
        response.setHeader("HX-Trigger",
            "{\"quizTimerSync\":{\"seconds\":"
            + quiz.getSegundosRestantes()
            + "}}");

        return "cuestionarios/questionQuiz :: quiz-content";
    }

    @PostMapping("/{slug}/timeout")
    public ResponseEntity<Void> timeoutQuiz(@PathVariable String slug, @AuthenticationPrincipal QuizlyUserDetails user,
            HttpSession session, HttpServletResponse response) {
        UserQuizSession quizSession = cuestionarioService.requireAccessibleQuizSession(slug, user.getId(), session);

        if (!quizSession.isFinished()) {
            cuestionarioService.finishQuiz(session);
        }

        return ResponseEntity
                .noContent()
                .header("HX-Redirect", "/cuestionarios/" + slug + "/summary")
                .build();
    }
    

    @GetMapping("/{slug}/summary")
    public String showQuestionSummary(@PathVariable String slug, @AuthenticationPrincipal QuizlyUserDetails user,
            HttpSession session, Model model, RedirectAttributes ra) {
        UserQuizSession quiz = cuestionarioService.requireAccessibleQuizSession(slug, user.getId(), session);

        if (!quiz.isFinished()) {
            toastService.info(ra, "No puede acceder al resumen antes de acabar el cuestionario.");
            return "redirect:/cuestionarios/" + slug + "/question";
        }

        model.addAttribute("summary", cuestionarioService.getSummary(quiz));

        return "cuestionarios/quizSummary";
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(QuizNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleInaccessibleQuiz() {
        return "error/404";
    }
}
