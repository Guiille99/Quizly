package com.quizly.app.repository.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.quizly.app.dto.form.UsuarioFilterForm;
import com.quizly.app.model.entity.Usuario;
import com.quizly.app.repository.UsuarioRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public class UsuarioRepositoryImpl implements UsuarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return entityManager.createQuery("SELECT u FROM Usuario u WHERE u.username = :username", Usuario.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return entityManager.createQuery("SELECT u FROM Usuario u WHERE u.email = :email", Usuario.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        return entityManager.createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.username = :username", Long.class)
                    .setParameter("username", username)
                    .getSingleResult() > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return entityManager.createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult() > 0;
    }

    @Override
    public Page<Usuario> findAllWithFilters(UsuarioFilterForm filters, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Consulta para obtener los datos
        CriteriaQuery<Usuario> query = cb.createQuery(Usuario.class);
        Root<Usuario> root = query.from(Usuario.class);
        query.select(root);

        List<Predicate> predicates = buildPredicates(cb, root, filters);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }

        // Aplicar ordenación basada en Pageable
        applySorting(cb, query, root, pageable);

        TypedQuery<Usuario> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Usuario> result = typedQuery.getResultList();

        // 2. Consulta para el total de elementos (respetando los filtros)
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Usuario> countRoot = countQuery.from(Usuario.class);
        countQuery.select(cb.count(countRoot));

        List<Predicate> countPredicates = buildPredicates(cb, countRoot, filters);
        if (!countPredicates.isEmpty()) {
            countQuery.where(countPredicates.toArray(Predicate[]::new));
        }

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // 3. Devolver la página construida
        return new PageImpl<>(result, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Usuario> root, UsuarioFilterForm filters) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters != null) {
            // Filtro general de búsqueda (aplica a nombre, apellidos, username o email)
            if (StringUtils.hasText(filters.getSearch())) {
                String search = "%" + filters.getSearch().trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("nombre")), search),
                    cb.like(cb.lower(root.get("apellidos")), search),
                    cb.like(cb.lower(root.get("username")), search),
                    cb.like(cb.lower(root.get("email")), search)
                );
                predicates.add(searchPredicate);
            }

            // Filtro exacto por Rol
            if (filters.getRol() != null) {
                predicates.add(cb.equal(root.get("rol"), filters.getRol()));
            }
        }

        return predicates;
    }

    private void applySorting(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Usuario> root, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            // Ordenación por defecto
            query.orderBy(cb.desc(root.get("createdAt")));
            return;
        }

        List<Order> orders = new ArrayList<>();

        for (Sort.Order sort : pageable.getSort()) {
            Expression<?> expression;

            switch (sort.getProperty()) {
                case "nombre":
                    expression = root.get("nombre");
                    break;
                case "apellidos":
                    expression = root.get("apellidos");
                    break;
                case "username":
                    expression = root.get("username");
                    break;
                case "email":
                    expression = root.get("email");
                    break;
                case "rol":
                    expression = root.get("rol");
                    break;
                case "intentos":
                    // Permite ordenar por la cantidad de cuestionarios realizados
                    expression = cb.size(root.get("intentos"));
                    break;
                default:
                    expression = root.get("createdAt");
            }

            orders.add(sort.isAscending() ? cb.asc(expression) : cb.desc(expression));
        }

        query.orderBy(orders);
    }
}
