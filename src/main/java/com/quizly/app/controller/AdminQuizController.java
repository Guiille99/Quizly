package com.quizly.app.controller;

import com.quizly.app.dto.form.QuizFilterForm;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.DificultadRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.repository.TipoPreguntaRepository;
import com.quizly.app.service.AdminQuizService;
import com.quizly.app.service.CuestionarioService;
import com.quizly.app.web.notification.ToastService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;




@Controller
@RequestMapping("/admin/cuestionarios")
@RequiredArgsConstructor
public class AdminQuizController {

    private final DificultadRepository dificultadRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final TemaRepository temaRepository;
    private final TipoPreguntaRepository tipoPreguntaRepository;
    private final CuestionarioRepository cuestionarioRepository;

    private final AdminQuizService adminQuizService;
    private final CuestionarioService cuestionarioService;
    private final ToastService toastService;

    @GetMapping
    public String showCuestionariosList(@ModelAttribute QuizFilterForm filters, Pageable pageable, Model model) {
        model.addAttribute("cuestionariosPage", adminQuizService.getCuestionarios(filters, pageable));
        model.addAttribute("filters", filters);
        model.addAttribute("asignaturas", asignaturaRepository.findAllByOrderByNombreAsc());

        return "admin/cuestionarios/cuestionarios-list";
    }

    @GetMapping("/tableData")
    public String getCuestionariosTableData(@ModelAttribute QuizFilterForm filters, Pageable pageable, Model model,
        HttpServletRequest request, HttpServletResponse response) {

        // Obtenemos el campo que ha lanzado el evento
        String triggeredField = request.getHeader("HX-Trigger-Name");

        // Si la asignatura cambia o pasa a filtrarse por todas se elimina el filtro por tema
        if (!"resetFilters".equals(triggeredField) 
            && ("asignaturaId".equals(triggeredField) || filters.asignaturaId() == null)) {
            filters = filters.toBuilder().temaId(null).build();
        }

        model.addAttribute("cuestionariosPage", adminQuizService.getCuestionarios(filters, pageable));
        model.addAttribute("filters", filters);

        
        // Si limpiamos los filtros devolvemos el fragmente del contenedor (filtros + tabla)
        // para que se actualicen ambas cosas
        if ("resetFilters".equals(triggeredField)) {
            model.addAttribute("asignaturas", asignaturaRepository.findAllByOrderByNombreAsc());
            return "admin/cuestionarios/cuestionarios-list :: table-cuestionarios-container_fragment";               
        }

        // Emitimos el evento de actualización de temas si cambió el filtro de asignatura
        if ("asignaturaId".equals(triggeredField)) {
            response.setHeader("HX-Trigger", "actualizarOpcionesTema");
        }
    

        return "admin/cuestionarios/cuestionarios-list :: table-cuestionarios_fragment";
    }

    @GetMapping("/opcionesTemaFiltro")
    public String getOpcionesTemasFiltro(@ModelAttribute QuizFilterForm filters, Model model) {
        // El filtro de los temas cargará los temas de la asignatura filtrada
        // Si el filtro por asignatura no está aplicado se devolverá una lista vacía
        model.addAttribute("temas", filters.asignaturaId() != null 
            ? temaRepository.findByAsignaturaId(filters.asignaturaId()) 
            : List.of());

        model.addAttribute("filters", filters);

        return "admin/cuestionarios/cuestionarios-list :: opciones-temas-filtro_fragment";
    }

    @GetMapping("/opcionesTema")
    public String getOpcionesTemasForm(@RequestParam(required = false) Long asignaturaId, Model model) {

        List<Tema> temas = asignaturaId == null ? List.of() : temaRepository.findByAsignaturaId(asignaturaId);

        model.addAttribute("temas", temas);

        return "admin/cuestionarios/quiz-form :: temas-options";
    }
    

    @GetMapping("/nuevo")
    public String showCreateQuizForm(Model model) {
        QuizForm quizForm = adminQuizService.createQuizForm();
        model.addAttribute("quizForm", quizForm);
        loadFormOptions(model, quizForm);

        return "admin/cuestionarios/quiz-form";
    }

    @PostMapping("/nuevo")
    public String createNewQuiz(@Valid @ModelAttribute("quizForm") QuizForm form,
        BindingResult bindingResult, Model model, RedirectAttributes ra) {
        
        if (bindingResult.hasErrors()) {
            loadFormOptions(model, form);
            return "admin/cuestionarios/quiz-form";
        }

        try {
            cuestionarioService.createQuiz(form);
            toastService.success(ra, "El cuestionario se ha creado correctamente.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            toastService.error(model, e.getMessage());
            loadFormOptions(model, form);
            return "admin/cuestionarios/quiz-form";
        } catch (DataIntegrityViolationException e) {
            toastService.error(model, "No se pudo crear el cuestionario por un conflicto de identificador interno. Inténtalo de nuevo.");
            loadFormOptions(model, form);
            return "admin/cuestionarios/quiz-form";
        } catch (Exception e){
            toastService.error(model, "Se ha producido un error al crear el cuestionario.");
            loadFormOptions(model, form);
            return "admin/cuestionarios/quiz-form";
        }

        return "redirect:/admin/cuestionarios";
    }
    

    @GetMapping("/{id}/editar")
    public String editarCuestionario(@PathVariable Long id, Model model) {
        Optional<Cuestionario> cuestionario = cuestionarioRepository.findById(id);

        if (!cuestionario.isPresent()) {
            toastService.error(model, "No ha encontrado el cuestionario solicitado");
            return "admin/cuestionarios/quiz-form";
        }
        
        QuizForm quizForm = adminQuizService.createQuizForm(cuestionario.get());
        model.addAttribute("quizForm", quizForm);
        loadFormOptions(model, quizForm);
        
        return "admin/cuestionarios/quiz-form";
    }

    private void loadFormOptions(Model model, QuizForm quizForm){
        model.addAttribute("asignaturas", asignaturaRepository.findAllByOrderByNombreAsc());
        model.addAttribute("dificultades", dificultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("tiposPregunta", tipoPreguntaRepository.findAllByOrderByNombreAsc());

        if (quizForm.getAsignaturaId() != null) {
            model.addAttribute("temas", temaRepository.findByAsignaturaId(quizForm.getAsignaturaId()));
        } else{
            model.addAttribute("temas", List.of());
        }
    }
    
    
}
