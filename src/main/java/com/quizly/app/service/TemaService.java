package com.quizly.app.service;

import com.quizly.app.repository.TemaRepository;
import java.util.List;

import org.springframework.stereotype.Service;

import com.quizly.app.dto.TemaEstadisticaDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemaService {

    private final TemaRepository temaRepository;

    /**
     * Obtiene los temas con mejor porcentaje de media
     * realizados por el usuario
     * @param usuarioId
     * @return
     */
    public List<TemaEstadisticaDto> getTemasMayorRendimientoUsuario(Long usuarioId){
        return temaRepository.getEstadisticasTemasPorUsuario(usuarioId).stream()
            .filter(t -> t.getPromedioAciertos() > 61.0)
            .sorted((t1, t2) -> t2.getPromedioAciertos().compareTo(t1.getPromedioAciertos()))
            .limit(3)
            .toList();
    }

    /**
     * Obtiene los temas con peor porcentaje de media
     * realizados por el usuario
     * 
     * @param usuarioId
     * @return
     */
    public List<TemaEstadisticaDto> getTemasPeorRendimientoUsuario(Long usuarioId) {
        return temaRepository.getEstadisticasTemasPorUsuario(usuarioId).stream()
                .filter(t -> t.getPromedioAciertos() <= 60.0)
                .sorted((t1, t2) -> t1.getPromedioAciertos().compareTo(t2.getPromedioAciertos()))
                .limit(3)
                .toList();
    }
}
