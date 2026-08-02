package com.quizly.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.dto.QuizFilterForm;
import com.quizly.app.mapper.QuizViewMapper;
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
}
