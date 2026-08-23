package com.quizly.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.mapper.TemaViewMapper;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.view.tema.TemaView;
import com.quizly.app.repository.TemaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminTemaService {
    private final TemaRepository temaRepository;
    private final TemaViewMapper temaViewMapper;

    public Page<TemaView> getTemas(Pageable pageable){
        Page<Tema> page = temaRepository.findAllWithFilters(pageable);
        return page.map(t -> temaViewMapper.toTemaView(t));
    }
}
