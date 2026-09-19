package com.quizly.app.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.dto.form.QuizFilterForm;
import com.quizly.app.mapper.QuizViewMapper;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.view.quiz.QuizView;
import com.quizly.app.repository.CuestionarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminQuizService {

    private final CuestionarioRepository cuestionarioRepository;
    private final QuizViewMapper quizViewMapper;

    public Page<QuizView> getCuestionarios(QuizFilterForm filters, Pageable pageable){
        Page<Cuestionario> page = cuestionarioRepository.findAll(filters, pageable);

        if (page.isEmpty()) {
            return page.map(c -> quizViewMapper.toQuizView(c));
        }

        // Obtenemos los ids de los cuestionarios mostrados en la página
        Set<Long> cuestionariosIds = page.getContent()
            .stream()
            .map(Cuestionario::getId)
            .collect(Collectors.toSet());

        // Obtenemos los ids de las últimas versiones de los cuestionarios
        Set<Long> ultimasVersionesIds = cuestionarioRepository.findLastVersionIds(cuestionariosIds);

        return page.map(c -> quizViewMapper.toQuizView(c, ultimasVersionesIds.contains(c.getId())));
    }
}
