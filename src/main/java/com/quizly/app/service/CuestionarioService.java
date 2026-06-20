package com.quizly.app.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.repository.CuestionarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuestionarioService {
    private final CuestionarioRepository cuestionarioRepository;

    public Page<Cuestionario> getCuestionariosPaginated(Pageable pageable){
       return cuestionarioRepository.findAllByOrderByTemaNombreAsc(pageable);
    }
}
