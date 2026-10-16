package com.quizly.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.quizly.app.dto.TemaProgresoDto;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.CuestionarioSerie;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.CuestionarioRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.repository.TemaRepository;

@ExtendWith(MockitoExtension.class)
class TemaServiceTest {
    @Mock
    private TemaRepository temaRepository;

    @Mock
    private IntentoCuestionarioRepository intentoCuestionarioRepository;

    @Mock
    private AsignaturaRepository asignaturaRepository;

    @Mock
    private CuestionarioRepository cuestionarioRepository;

    @Mock
    private SlugService slugService;

    @InjectMocks
    private TemaService temaService;

    @Test
    void topicCardCountsOnlyLatestCommonAndOwnQuizzes() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);

        Asignatura asignatura = new Asignatura();
        asignatura.setId(20L);

        Cuestionario comun = createQuiz(1L, 101L, 1, null);
        Cuestionario propio = createQuiz(2L, 102L, 1, 10L);
        Cuestionario ajeno = createQuiz(3L, 103L, 1, 30L);
        Cuestionario versionAntigua = createQuiz(4L, 104L, 1, null);

        Tema tema = new Tema();
        tema.setCuestionarios(List.of(comun, propio, ajeno, versionAntigua));

        PageRequest pageable = PageRequest.of(0, 10);
        when(temaRepository.findByAsignaturaId(20L, pageable))
                .thenReturn(new PageImpl<>(List.of(tema), pageable, 1));
        when(cuestionarioRepository.findLastVersionIds(Set.of(1L, 2L, 3L, 4L)))
                .thenReturn(Set.of(1L, 2L, 3L));
        when(intentoCuestionarioRepository.findByUsuarioId(10L)).thenReturn(List.of());

        TemaProgresoDto result = temaService
                .getTemasProgresoPaginatedByUsuarioYAsignatura(usuario, asignatura, null, pageable)
                .getContent()
                .get(0);

        assertEquals(2, result.totalCuestionarios());
        assertEquals(0, result.porcentaje());
    }

    private static Cuestionario createQuiz(Long id, Long serieId, Integer version, Long ownerId) {
        CuestionarioSerie serie = new CuestionarioSerie();
        serie.setId(serieId);

        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setId(id);
        cuestionario.setSerie(serie);
        cuestionario.setVersion(version);
        if (ownerId != null) {
            Usuario owner = new Usuario();
            owner.setId(ownerId);
            cuestionario.setUsuario(owner);
        }
        return cuestionario;
    }
}