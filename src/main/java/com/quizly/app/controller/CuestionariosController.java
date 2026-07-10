package com.quizly.app.controller;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.session.AnswerRequest;
import com.quizly.app.model.session.UserQuizSession;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.security.QuizlyUserDetails;
import com.quizly.app.service.CuestionarioService;
import com.quizly.app.service.QuizSessionService;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quizly.app.util.Constantes;
import com.quizly.app.web.notification.ToastService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;







@Controller
@RequiredArgsConstructor
@RequestMapping("/cuestionarios")
public class CuestionariosController {

    private final CuestionarioService cuestionarioService;
    private final QuizSessionService quizSessionService;


    @GetMapping
    public String showTodosCuestionarios(Model model, HttpServletRequest request, 
            @PageableDefault(size = 5) Pageable pageable) {
        
        Page<Cuestionario> cuestionarios = cuestionarioService.getCuestionariosPaginated(pageable);

        model.addAttribute("cuestionariosPage", cuestionarios);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        return "cuestionarios/cuestionarios-todos";
    }

    @GetMapping("/recientes")
    public String showCuestionariosRecientes(Model model, HttpServletRequest request,
           @AuthenticationPrincipal QuizlyUserDetails authUser, 
           @PageableDefault(size = 10) Pageable pageable) {
        
        Page<Cuestionario> cuestionariosRecientes = cuestionarioService.getCuestionariosRecientesPaginated(pageable, authUser);
        
        model.addAttribute("cuestionariosRecientesPage", cuestionariosRecientes);
        model.addAttribute(Constantes.ATTR_CURRENT_URI, request.getRequestURI());
        return "cuestionarios/cuestionarios-recientes";
    }
    
 
    
    @GetMapping("/table-data")
    public String getCuestionariosDatosTabla(Model model, 
        @PageableDefault(size = 5) Pageable pageable) {
        
        Page<Cuestionario> cuestionarios = cuestionarioService.getCuestionariosPaginated(pageable);

        model.addAttribute("cuestionariosPage", cuestionarios);

        return "cuestionarios/cuestionarios-todos :: lista-cuestionarios_fragment";
    }

    @GetMapping("/recientes/table-data")
    public String getCuestionariosRecientesDatosTabla(Model model, @AuthenticationPrincipal QuizlyUserDetails authUser,
        @PageableDefault(size = 10) Pageable pageable) {
        
        Page<Cuestionario> cuestionariosRecientes = cuestionarioService.getCuestionariosRecientesPaginated(pageable, authUser);

        model.addAttribute("cuestionariosRecientesPage", cuestionariosRecientes);

        return "cuestionarios/cuestionarios-recientes :: lista-cuestionarios_fragment";
    }
    

    @GetMapping("/{slug}")
    public String showStart(@PathVariable String slug, Model model) {
        Optional<Cuestionario> cuestionario = cuestionarioService.getCuestionarioBySlug(slug);

        if (cuestionario.isPresent()) {
            model.addAttribute("quiz", cuestionario.get());
        }
        
        return "cuestionarios/quizStart";
    }

    @PostMapping("/{slug}/start")
    public String startQuiz(@PathVariable String slug, @AuthenticationPrincipal QuizlyUserDetails user, HttpSession session) {
        cuestionarioService.startQuiz(slug, user.getId(), session);

        return "redirect:/cuestionarios/" + slug + "/question";
    }
    
    @GetMapping("/{slug}/question")
    public String showQuizQuestion(@PathVariable String slug, HttpSession session, Model model) {

        UserQuizSession quizSession = quizSessionService.getRequired(session);

        if (quizSession.isFinished()) {
            return "redirect:/cuestionarios/" + slug + "/summary";
        }

        model.addAttribute("quiz", quizSession);
        model.addAttribute("question", quizSession.getCurrentQuestion());

       return "cuestionarios/questionQuiz";
    }
    
    @PostMapping("/{slug}/answer")
    public String answerQuizQuestion(@PathVariable String slug, @ModelAttribute AnswerRequest request,
        HttpSession session, HttpServletResponse response, Model model) {

        cuestionarioService.answerQuestion(request.optionId(), session);
        UserQuizSession quiz = quizSessionService.getRequired(session);

        if (quiz.isFinished()) {
            cuestionarioService.finishQuiz(session);
            response.setHeader("HX-Redirect", "/cuestionarios/" + slug + "/summary");
            return null;
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("question", quiz.getCurrentQuestion());

        return "cuestionarios/questionQuiz :: question-card";
    }

    @GetMapping("/{slug}/summary")
    public String showQuestionSummary(@PathVariable String slug, HttpSession session, Model model) {
        UserQuizSession quiz = quizSessionService.getRequired(session);

        if (!quiz.isFinished()) {
            return "redirect:/cuestionarios/" + slug + "/question";
        }

        model.addAttribute("summary", cuestionarioService.getSummary(quiz));

        return "cuestionarios/quizSummary";
    }
}
