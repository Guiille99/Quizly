package com.quizly.app.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.repository.IntentoCuestionarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntentoCuestionarioService {
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;

    public Page<IntentoCuestionario> getIntentosCuestionariosByUsuarioYEstadoPaginated(Long userId, String codigoEstadoIntento, 
            Pageable pageable){
        return intentoCuestionarioRepository.getIntentosCuestionariosByUsuarioYEstado(userId, codigoEstadoIntento, pageable);
    }
}
