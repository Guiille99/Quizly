package com.quizly.app.repository.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import com.quizly.app.dto.form.AsignaturaFilterForm;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.repository.AsignaturaRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class AsignaturaRepositoryImpl implements AsignaturaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Asignatura> findBySlug(String slug) {
        return entityManager.createQuery("SELECT a FROM Asignatura a WHERE a.slug = :slug", Asignatura.class)
                .setParameter("slug", slug)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsBySlug(String slug) {
        Long count = entityManager.createQuery("SELECT COUNT(a) FROM Asignatura a WHERE a.slug = :slug", Long.class)
                .setParameter("slug", slug)
                .getSingleResult();
        
        return count > 0;
    }

    @Override
    public Page<Asignatura> findAllWithFilters(AsignaturaFilterForm filters, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Consulta para obtener los datos
        CriteriaQuery<Asignatura> query = cb.createQuery(Asignatura.class);
        Root<Asignatura> root = query.from(Asignatura.class);
        query.select(root);

        List<Predicate> predicates = buildPredicates(cb, root, filters);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }

        // Aplicar ordenación basada en el objeto Pageable
        applySorting(cb, query, root, pageable);

        TypedQuery<Asignatura> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Asignatura> result = typedQuery.getResultList();

        // 2. Consulta para obtener el total de elementos
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Asignatura> countRoot = countQuery.from(Asignatura.class);
        countQuery.select(cb.count(countRoot));

        // Aplicar los mismos predicados al count para que la paginación no se rompa al filtrar
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, filters);
        if (!countPredicates.isEmpty()) {
            countQuery.where(countPredicates.toArray(Predicate[]::new));
        }

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // 3. Devolver la página
        return new PageImpl<>(result, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Asignatura> root, AsignaturaFilterForm filters) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters != null && StringUtils.hasText(filters.getSearch())) {
            String search = "%" + filters.getSearch().trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("nombre")), search));
        }

        return predicates;
    }

    private void applySorting(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Asignatura> root, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            query.orderBy(cb.asc(root.get("nombre")));
            return;
        }

        List<Order> orders = new ArrayList<>();

        for (Sort.Order sort : pageable.getSort()) {
            Expression<?> expression;

            switch (sort.getProperty()) {
                case "nombre":
                    expression = root.get("nombre");
                    break;
                case "descripcion":
                    expression = root.get("descripcion");
                    break;
                case "nTemas":
                    // Ordenamos por el tamaño de la lista de temas asociada
                    expression = cb.size(root.get("temas"));
                    break;
                default:
                    expression = root.get("nombre");
            }

            orders.add(sort.isAscending() ? cb.asc(expression) : cb.desc(expression));
        }

        query.orderBy(orders);
    }
}