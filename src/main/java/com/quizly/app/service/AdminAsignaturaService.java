package com.quizly.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.dto.form.AsignaturaFilterForm;
import com.quizly.app.dto.form.AsignaturaForm;
import com.quizly.app.mapper.AsignaturaViewMapper;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.view.asignatura.AsignaturaView;
import com.quizly.app.repository.AsignaturaRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AdminAsignaturaService {
    private final AsignaturaRepository asignaturaRepository;

    private final AsignaturaViewMapper asignaturaViewMapper;

    public Page<AsignaturaView> getAsignaturas(AsignaturaFilterForm filters, Pageable pageable){
        Page<Asignatura> page = asignaturaRepository.findAllWithFilters(filters, pageable);
        return page.map(asignaturaViewMapper::toAsignaturaView);
    }

    public AsignaturaForm createAsignaturaForm(){
        return new AsignaturaForm();
    }

    public AsignaturaForm createAsignaturaForm(Asignatura asignatura){
        AsignaturaForm form = new AsignaturaForm();
        form.setId(asignatura.getId());
        form.setNombre(asignatura.getNombre());
        form.setDescripcion(asignatura.getDescripcion());
        return form;
    }
}
