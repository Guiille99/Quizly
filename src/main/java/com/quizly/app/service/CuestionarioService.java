package com.quizly.app.service;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.security.QuizlyUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuestionarioService {
    private final CuestionarioRepository cuestionarioRepository;

    public Page<Cuestionario> getCuestionariosPaginated(Pageable pageable){
       return cuestionarioRepository.findAllByOrderByTemaNombreAsc(pageable);
    }

    public Page<Cuestionario> getCuestionariosRecientesPaginated(Pageable pageable, QuizlyUserDetails authUser){
        List<String> intentosCuestionarioEstados = List.of(
            IntentoCuestionarioEstadoEnum.FINISHED.toString(), 
            IntentoCuestionarioEstadoEnum.ABANDONNED.toString()
        );

        return cuestionarioRepository.getCuestionariosRecientesByUsuarioYEstado(authUser.getId(), intentosCuestionarioEstados, pageable);
    }
}
