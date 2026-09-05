package com.quizly.app.service;

import com.quizly.app.repository.AsignaturaRepository;
import com.quizly.app.repository.IntentoCuestionarioRepository;
import com.quizly.app.repository.TemaRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.quizly.app.dto.TemaEstadisticaDto;
import com.quizly.app.dto.TemaProgresoDto;
import com.quizly.app.dto.form.TemaForm;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemaService {

    private final TemaRepository temaRepository;
    private final IntentoCuestionarioRepository intentoCuestionarioRepository;
    private final AsignaturaRepository asignaturaRepository;

    private final SlugService slugService;


    @Transactional 
    public Tema createTema(TemaForm form){
        Tema tema = new Tema();

        Asignatura asignatura = asignaturaRepository.findById(form.getAsignaturaId())
            .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado la asignatura seleccionada"));

        validateTema(form);

        tema.setNombre(form.getNombre());
        tema.setOrden(form.getOrden());
        tema.setDescripcion(form.getDescripcion());
        tema.setSlug(resolveUniqueSlug(form.getNombre()));


        tema.setAsignatura(asignatura);

        tema = temaRepository.save(tema);

        return tema;

    }

    private void validateTema(TemaForm form){
        if (temaRepository.existsByAsignaturaIdAndOrden(form.getAsignaturaId(), form.getOrden())) {
            throw new IllegalArgumentException("El número de tema " + form.getOrden() + " ya está ocupado por otro tema en esa asignatura.");
        }

        if (temaRepository.existsByAsignaturaIdAndNombreIgnoreCase(form.getAsignaturaId(), form.getNombre())) {
            throw new IllegalArgumentException("Ya existe un tema con ese nombre en la asignatura seleccionada.");
        }
    }

    private String resolveUniqueSlug(String txt) {
        String baseSlug = slugService.generateSlug(txt);
        if (!temaRepository.existsBySlug(baseSlug)) {
            return baseSlug;
        }

        int suffix = 2;
        String candidate;
        do {
            candidate = baseSlug + "-" + suffix++;
        } while (temaRepository.existsBySlug(candidate));

        return candidate;
    }

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

    /**
     * Obtiene todos los temas de la asignatura
     * pasada por parámetro
     * 
     * @param asignatura
     * @return List<{@link Tema}>
     */
    public Page<TemaProgresoDto> getTemasProgresoPaginatedByUsuarioYAsignatura(Usuario usuario, Asignatura asignatura, 
            String nameFilter, Pageable pageable) {

        if (asignatura == null) {
            return Page.empty();
        }

        // Gestionar la ordenación
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("nombre").ascending();
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Tema> temasPage;
        if (StringUtils.isNotBlank(nameFilter)) {
            temasPage = temaRepository.findByNombreContainingIgnoreCaseAndAsignaturaId(nameFilter, asignatura.getId(), pageable);
        } else{
            temasPage = temaRepository.findByAsignaturaId(asignatura.getId(), pageable);
        }

        // Almacenamos los IDs de los cuestionarios completados por el usuario
        Set<Long> idsCuestionariosCompletadosAux = Set.of();
        if (usuario != null) {
            idsCuestionariosCompletadosAux = intentoCuestionarioRepository.findByUsuarioId(usuario.getId()).stream()
                .filter(i -> IntentoCuestionarioEstadoEnum.FINISHED.toString().equals(i.getEstado().getCodigo()))
                .map(i -> i.getCuestionario().getId())
                .collect(Collectors.toSet());
        }

        final Set<Long> idsCuestionariosCompletados = idsCuestionariosCompletadosAux;

        List<TemaProgresoDto> temasProgreso = temasPage.stream().map(tema -> {
            List<Cuestionario> cuestionarios = tema.getCuestionarios();
            int totalCuestionarios = cuestionarios.size();
            long nCuestionariosCompletados = cuestionarios.stream()
                .filter(c -> idsCuestionariosCompletados.contains(c.getId()))
                .count();

            int porcentaje = totalCuestionarios > 0 ? (int) (nCuestionariosCompletados * 100 / totalCuestionarios) : 0;

            return new TemaProgresoDto(tema, porcentaje);
        }).toList();

        return new PageImpl<>(temasProgreso, sortedPageable, temasPage.getTotalElements());
    }
    

    /**
     * Obtiene el tema asociado al slug pasado por parámeto.
     * En caso de no existir devolverá null.
     * @param slug
     * @return {@link Tema}
     */
    public Tema getTemaBySlug(String slug){
        return temaRepository.findBySlug(slug).orElse(null);
    }
}
