package com.quizly.app.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.quizly.app.dto.AsignaturaProgresoDto;
import com.quizly.app.dto.form.AsignaturaForm;
import com.quizly.app.exception.QuizlyException;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsignaturaService {
    private final AsignaturaRepository asignaturaRepository;
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;

    private final SlugService slugService;


    @Transactional
    public Asignatura createAsignatura(AsignaturaForm form){
        validateAsignatura(form);

        Asignatura asignatura = new Asignatura();
        asignatura.setNombre(form.getNombre());
        asignatura.setDescripcion(form.getDescripcion());
        asignatura.setSlug(resolveUniqueSlug(form.getNombre()));

        return asignaturaRepository.save(asignatura);
    }

    @Transactional
    public Asignatura updateAsignatura(Long id, AsignaturaForm form){
        Asignatura asignatura = asignaturaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado la asignatura a actualizar."));
        
        validateAsignatura(form);

        asignatura.setNombre(form.getNombre());
        asignatura.setDescripcion(form.getDescripcion());
        asignatura.setSlug(resolveUniqueSlug(form.getNombre()));

        return asignaturaRepository.save(asignatura);
    }

    @Transactional 
    public void deleteAsignatura(Long id) throws QuizlyException {
        Asignatura asignatura = asignaturaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado la asignatura solicitada."));

        if (!CollectionUtils.isEmpty(asignatura.getTemas())) {
            throw new QuizlyException("No se puede eliminar la asignatura porque tiene temas asociados. Elimine primero los temas.");
        }

        asignaturaRepository.delete(asignatura);
    }

    private void validateAsignatura(AsignaturaForm form){
        Long id = form.getId() != null ? form.getId() : -1L;

        // Comprobar que no exista otra asignatura con el mismo nombre
        if (asignaturaRepository.existsByNombreIgnoreCaseAndIdNot(form.getNombre(), id)) {
            throw new IllegalArgumentException("Ya existe una asignatura con ese nombre.");
        }
    }

        private String resolveUniqueSlug(String txt) {
        String baseSlug = slugService.generateSlug(txt);
        if (!asignaturaRepository.existsBySlug(baseSlug)) {
            return baseSlug;
        }

        int suffix = 2;
        String candidate;
        do {
            candidate = baseSlug + "-" + suffix++;
        } while (asignaturaRepository.existsBySlug(candidate));

        return candidate;
    }

    /**
     * Obtiene una lista de {@link AsignaturaProgresoDto}
     * para el usuario recibido por parámetro
     * @param usuario
     * @return List<{@link AsignaturaProgresoDto}>
     */
    public Page<AsignaturaProgresoDto> getAsignaturasProgresoUsuarioPaginated(Usuario usuario, String nameFilter, Pageable pageable){
        // Gestionar la ordenación
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("nombre").ascending();
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // Filtro por nombre de la asignatura
        Page<Asignatura> paginaAsignaturas;
        if (StringUtils.isNotBlank(nameFilter)) {
            paginaAsignaturas = asignaturaRepository.findByNombreContainingIgnoreCase(nameFilter, sortedPageable);
        } else{
            paginaAsignaturas = asignaturaRepository.findAll(sortedPageable);
        }

        Set<Long> idsCuestionariosCompletadosAux = Set.of();
        if (usuario != null) {
            idsCuestionariosCompletadosAux = intentoCuestionarioRepository.findByUsuarioId(usuario.getId()).stream()
                .filter(i -> IntentoCuestionarioEstadoEnum.FINISHED.toString().equals(i.getEstado().getCodigo()))
                .map(i -> i.getCuestionario().getId())
                .collect(Collectors.toSet());
        }

        final Set<Long> idsCuestionariosCompletados = idsCuestionariosCompletadosAux;

        List<AsignaturaProgresoDto> asignaturasProgreso = paginaAsignaturas.stream().map(asig -> {
            long nTemas = asig.getTemas().size();
            List<Cuestionario> cuestionarios = asig.getTemas().stream()
                .flatMap(t -> t.getCuestionarios().stream())
                .toList();

            long totalCuestionarios = cuestionarios.size();
            long nCuestionariosCompletados = cuestionarios.stream()
                .filter(c -> idsCuestionariosCompletados.contains(c.getId()))
                .count();

            int porcentaje = totalCuestionarios > 0 ? (int) (nCuestionariosCompletados * 100 / totalCuestionarios) : 0;
            return new AsignaturaProgresoDto(asig, nTemas, porcentaje);
        }).toList();

        return new PageImpl<>(asignaturasProgreso, sortedPageable, paginaAsignaturas.getTotalElements());
    }

    /**
     * Obtiene la asignatura a través de su slug.
     * En caso de no existir devolverá null.
     * @param slug
     * @return {@link Asignatura}
     */
    public Asignatura getAsignatruaBySlug(String slug){
        return asignaturaRepository.findBySlug(slug).orElse(null);
    }
}
