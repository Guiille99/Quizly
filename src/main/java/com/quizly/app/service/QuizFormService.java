package com.quizly.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.quizly.app.dto.form.OpcionRespuestaForm;
import com.quizly.app.dto.form.PreguntaForm;
import com.quizly.app.dto.form.QuizForm;
import com.quizly.app.dto.form.QuizFormOptions;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Dificultad;
import com.quizly.app.model.entity.OpcionRespuesta;
import com.quizly.app.model.entity.Pregunta;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.entity.TipoPregunta;
import com.quizly.app.model.enums.UnidadTiempo;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.DificultadRepository;
import com.quizly.app.repository.TemaRepository;
import com.quizly.app.repository.TipoPreguntaRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class QuizFormService {

    private final AsignaturaRepository asignaturaRepository;
    private final DificultadRepository dificultadRepository;
    private final TemaRepository temaRepository;
    private final TipoPreguntaRepository tipoPreguntaRepository;

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
        form.setTemaId(cuestionario.getTema() != null ? cuestionario.getTema().getId() : null);
        form.setAsignaturaId(cuestionario.getTema() != null ? cuestionario.getTema().getAsignatura().getId() : null);
        form.setDificultadId(cuestionario.getDificultad().getId());
        form.setDescripcion(cuestionario.getDescripcion());
        mapTiempoLimite(cuestionario, form);

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
    
    /**
     * Obtiene todas las opciones necesarias para renderizar el formulario
     * @param asignaturaId
     * @return
     */
    public QuizFormOptions getOptions(Long asignaturaId){
        List<Asignatura> asignaturas = asignaturaRepository.findAllByOrderByNombreAsc();
        List<Dificultad> dificultades = dificultadRepository.findAllByOrderByNombreAsc();
        List<TipoPregunta> tiposPregunta = tipoPreguntaRepository.findAllByOrderByNombreAsc();
        List<Tema> temas = asignaturaId == null ? List.of() : temaRepository.findByAsignaturaId(asignaturaId);

        return new QuizFormOptions(asignaturas, temas, dificultades, tiposPregunta);
    }

    /**
     * Convierte el tiempo límite almacenado en segundos
     * a la representación utilizada por el formulario
     * @param cuestionario
     * @param form
     */
    private void mapTiempoLimite(Cuestionario cuestionario, QuizForm form){
        
        Integer segundos = cuestionario.getTiempoLimite();

        if (segundos == null) {
            form.setSinLimiteTiempo(true);
            form.setTiempoLimite(null);
            return;
        }

        form.setSinLimiteTiempo(false);

        if (segundos % 3600 == 0) {
            form.setTiempoLimite(segundos/3600);
            form.setUnidadTiempo(UnidadTiempo.HORAS);
        } else if(segundos % 60 == 0){
            form.setTiempoLimite(segundos/60);
            form.setUnidadTiempo(UnidadTiempo.MINUTOS);
        } else{
            form.setTiempoLimite(segundos);
            form.setUnidadTiempo(UnidadTiempo.SEGUNDOS);
        }
    }
}
