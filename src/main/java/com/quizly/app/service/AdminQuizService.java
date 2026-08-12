package com.quizly.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.dto.form.OpcionRespuestaForm;
import com.quizly.app.dto.form.PreguntaForm;
import com.quizly.app.dto.form.QuizFilterForm;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.mapper.QuizViewMapper;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.OpcionRespuesta;
import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.model.enums.UnidadTiempo;
import com.quizly.app.model.view.quiz.QuizView;
import com.quizly.app.repository.CuestionarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminQuizService {

    private final CuestionarioRepository cuestionarioRepository;
    private final QuizViewMapper quizViewMapper;

    public Page<QuizView> getCuestionarios(QuizFilterForm filters, Pageable pageable){
        return cuestionarioRepository.findAll(filters, pageable)
            .map(c -> quizViewMapper.toQuizView(c));
    }

    /**
     * Prepara un formulario vacío para la creación de un cuestionario
     * 
     * @return formulario de cuestionario con sus valores iniciales
     */
    public QuizForm createQuizForm(){
        QuizForm form = new QuizForm();

        form.setPreguntasAleatorias(false);
        form.setTiempoLimite(1);
        form.setSinLimiteTiempo(true);
        form.setUnidadTiempo(UnidadTiempo.MINUTOS);

        return form;
    }

    /**
     * Prepara el formulario de edición del cuestionario
     * pasado por parámetro.
     * 
     * @param cuestionario
     * @return
     */
    public QuizForm createQuizForm(Cuestionario cuestionario){
        QuizForm form = new QuizForm();
        List<PreguntaForm> preguntasForm = new ArrayList<>();

        form.setId(cuestionario.getId());
        form.setTitulo(cuestionario.getTitulo());
        form.setTemaId(cuestionario.getTema().getId());
        form.setAsignaturaId(cuestionario.getTema().getAsignatura().getId());
        form.setDificultadId(cuestionario.getDificultad().getId());
        form.setTiempoLimite(cuestionario.getTiempoLimite());
        form.setUnidadTiempo(UnidadTiempo.SEGUNDOS);
        form.setSinLimiteTiempo(cuestionario.getTiempoLimite() == null);
        form.setDescripcion(cuestionario.getDescripcion());

        // Se añaden las preguntas
        for (Pregunta pregunta : cuestionario.getPreguntas()) {
            PreguntaForm preguntaForm = new PreguntaForm();
            List<OpcionRespuestaForm> opcionesRespuestaForm = new ArrayList<>();

            preguntaForm.setEnunciado(pregunta.getEnunciado());
            preguntaForm.setTipoPreguntaCodigo(pregunta.getTipoPregunta().getCodigo());

            // Se añaden las opciones de repsuesta de cada pregunta
            for (OpcionRespuesta opcionRespuesta : pregunta.getOpcionesRespuesta()) {
                OpcionRespuestaForm opcionRespuestaForm = new OpcionRespuestaForm();

                opcionRespuestaForm.setTexto(opcionRespuesta.getTexto());
                opcionRespuestaForm.setCorrecta(opcionRespuesta.getCorrecta());
                opcionRespuestaForm.setRetroalimentacion(opcionRespuesta.getRetroalimentacion());

                opcionesRespuestaForm.add(opcionRespuestaForm);
            }

            preguntaForm.setOpcionesRespuesta(opcionesRespuestaForm);

            preguntasForm.add(preguntaForm);
        }

        form.setPreguntas(preguntasForm);
        form.setPreguntasAleatorias(cuestionario.isPreguntasAleatorias());

        return form;
    }
    

}
