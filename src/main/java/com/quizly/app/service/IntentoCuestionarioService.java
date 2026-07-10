package com.quizly.app.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.IntentoCuestionario;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.repository.IntentoCuestionarioEstadoRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntentoCuestionarioService {
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;
    private final IntentoCuestionarioEstadoRepository intentoCuestionarioEstadoRepository;

    public Page<IntentoCuestionario> getIntentosCuestionariosByUsuarioYEstadoPaginated(Long userId, String codigoEstadoIntento, 
            Pageable pageable){
        return intentoCuestionarioRepository.getIntentosCuestionariosByUsuarioYEstado(userId, codigoEstadoIntento, pageable);
    }

    public IntentoCuestionario crearIntentoCuestionario(Cuestionario cuestionario, Usuario usuario){
        IntentoCuestionario intentoCuestionario = new IntentoCuestionario();
        
        intentoCuestionario.setCuestionario(cuestionario);
        intentoCuestionario.setUsuario(usuario);
        intentoCuestionario.setFechaInicio(LocalDateTime.now());
        intentoCuestionario.setAciertos(0);
        intentoCuestionario.setFallos(0);
        intentoCuestionario.setTiempoEmpleado(0L);
        intentoCuestionario.setEstado(intentoCuestionarioEstadoRepository.findByCodigo(IntentoCuestionarioEstadoEnum.IN_PROGRESS.toString()).get());

        return intentoCuestionarioRepository.save(intentoCuestionario);
    }
}
