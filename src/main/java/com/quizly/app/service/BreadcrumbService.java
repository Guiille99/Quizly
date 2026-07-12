package com.quizly.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.view.breadcrumb.BreadcrumbItem;

@Service
public class BreadcrumbService {

    /**
     * Ej: Asignaturas
     * @return
     */
    public List<BreadcrumbItem> asignaturas(){
        return List.of(
            new BreadcrumbItem("Asignaturas", "/asignaturas")
        );
    }

    /**
     * Ej: Asignaturas > 'Nombre asignatura'
     * 
     * @param asignatura
     * @return
     */
    public List<BreadcrumbItem> asignatura(Asignatura asignatura){
        return List.of(
            new BreadcrumbItem("Asignaturas", "/asignaturas"),
            new BreadcrumbItem(asignatura.getNombre(), null)
        );
    }

    /**
     * Ej: Asignaturas > 'Nombre asignatura' > 'Nombre tema'
     * @param tema
     * @return
     */
    public List<BreadcrumbItem> temaAsignatura(Tema tema){
        return List.of(
            new BreadcrumbItem("Asignaturas", "/asignaturas"),
            new BreadcrumbItem(tema.getAsignatura().getNombre(), "/asignaturas/" + tema.getAsignatura().getSlug()),
            new BreadcrumbItem(tema.getNombre(), null)
        );
    }
}
