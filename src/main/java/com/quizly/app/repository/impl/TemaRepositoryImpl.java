package com.quizly.app.repository.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import com.quizly.app.dto.TemaEstadisticaDto;
import com.quizly.app.dto.form.TemaFilterForm;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.model.enums.IntentoCuestionarioEstadoEnum;
import com.quizly.app.repository.TemaRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class TemaRepositoryImpl implements TemaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Tema> findBySlug(String slug) {
        TypedQuery<Tema> query = entityManager.createQuery(
            "SELECT t FROM Tema t WHERE t.slug = :slug", Tema.class);
        query.setParameter("slug", slug);
        return query.getResultStream().findFirst();
    }

    @Override
    public List<Tema> findByAsignaturaId(Long asignaturaId) {
        TypedQuery<Tema> query = entityManager.createQuery(
            "SELECT t FROM Tema t WHERE t.asignatura.id = :asignaturaId ORDER BY t.orden ASC", 
            Tema.class);
        query.setParameter("asignaturaId", asignaturaId);
        return query.getResultList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(t) FROM Tema t WHERE t.slug = :slug", Long.class)
            .setParameter("slug", slug)
            .getSingleResult();
        
        return count > 0;
    }

    @Override
    public List<TemaEstadisticaDto> getEstadisticasTemasPorUsuario(Long usuarioId) {
       return entityManager.createQuery("""
                SELECT new com.quizly.app.dto.TemaEstadisticaDto(
                    t.id,
                    t.nombre, 
                    AVG(CAST(ic.aciertos AS double) * 100 / NULLIF(ic.aciertos + ic.fallos, 0))
                )
                FROM IntentoCuestionario ic
                JOIN ic.cuestionario c
                JOIN  c.tema t
                WHERE ic.usuario.id = :usuarioId
                AND ic.estado.codigo = :estadoIntento
                GROUP BY t
               """, TemaEstadisticaDto.class)
               .setParameter("usuarioId", usuarioId)
               .setParameter("estadoIntento", IntentoCuestionarioEstadoEnum.FINISHED.toString())
               .getResultList();
    }

    @Override
    public Page<Tema> findAllWithFilters(TemaFilterForm filters, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Consulta para obtener los datos
        CriteriaQuery<Tema> query = cb.createQuery(Tema.class);
        Root<Tema> root = query.from(Tema.class);
        query.select(root);

        List<Predicate> predicates = buildPredicates(cb, root, filters);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }

        // Aplicar ordenación basada en el objeto Pageable
        applySorting(cb, query, root, pageable);

        TypedQuery<Tema> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Tema> result = typedQuery.getResultList();

        // Consulta para obtener el total de elementos
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Tema> counRoot = countQuery.from(Tema.class);
        countQuery.select(cb.count(counRoot));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // Devolver la página
        return new PageImpl<>(result, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Tema> root, TemaFilterForm filters){
        List<Predicate> predicates = new ArrayList<>();

        if (filters != null) {
            // Filtro por nombre
            if (StringUtils.hasText(filters.search())){
                String search = "%" + filters.search().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("nombre")), search));
            }

            // Filtro por asignatura
            if (filters.asignaturaId() != null) {
                Join<Tema, Asignatura> asignatura = root.join("asignatura");
                predicates.add(cb.equal(asignatura.get("id"), filters.asignaturaId()));
            }
        }

        return predicates;
    }

    private void applySorting(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Tema> root, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            query.orderBy(cb.desc(root.get("updatedAt")));
        }

        List<Order> orders = new ArrayList<>();

        Join<Tema, Asignatura> asignatura = null;

        for (Sort.Order sort : pageable.getSort()) {
            Expression<?> expression;

            switch (sort.getProperty()) {
                case "nombre":
                    expression = root.get("nombre");
                    break;
                case "orden":
                    expression = root.get("orden");
                    break;

                case "asignatura":
                    if (asignatura == null) {
                        asignatura = root.join("asignatura");
                    }

                    expression = asignatura.get("nombre");
                    break;

                case "nCuestionarios":
                    expression = cb.size(root.get("cuestionarios"));
                    break;
                default:
                    expression = root.get("updatedAt");
            }

            orders.add(sort.isAscending() ? cb.asc(expression) : cb.desc(expression));
        }

        query.orderBy(orders);
    }
}