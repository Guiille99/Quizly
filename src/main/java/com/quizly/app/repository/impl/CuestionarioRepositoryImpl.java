package com.quizly.app.repository.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.quizly.app.dto.QuizFilterForm;
import com.quizly.app.model.entity.Asignatura;
import com.quizly.app.model.entity.Cuestionario;
import com.quizly.app.model.entity.Dificultad;
import com.quizly.app.model.entity.Tema;
import com.quizly.app.repository.CuestionarioRepositoryCustom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

public class CuestionarioRepositoryImpl implements CuestionarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Cuestionario> findByTemaId(Long temaId) {
        TypedQuery<Cuestionario> query = entityManager.createQuery(
            "SELECT c FROM Cuestionario c WHERE c.tema.id = :temaId", Cuestionario.class);
        query.setParameter("temaId", temaId);
        return query.getResultList();
    }

    @Override
    public List<Cuestionario> findByTemaIdAndDificultadId(Long temaId, Long dificultadId) {
        TypedQuery<Cuestionario> query = entityManager.createQuery(
            "SELECT c FROM Cuestionario c WHERE c.tema.id = :temaId AND c.dificultad.id = :dificultadId", 
            Cuestionario.class);
        query.setParameter("temaId", temaId);
        query.setParameter("dificultadId", dificultadId);
        return query.getResultList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(c) FROM Cuestionario c WHERE c.slug = :slug", Long.class)
            .setParameter("slug", slug)
            .getSingleResult();
        return count > 0;
    }

    @Override
    public Optional<Cuestionario> findLastVersionBySlug(String slug) {
        TypedQuery<Cuestionario> query = entityManager.createQuery("""
            SELECT c FROM Cuestionario c WHERE c.slug = :slug ORDER BY c.version DESC
        """, Cuestionario.class);

        query.setParameter("slug", slug);
        return query.getResultStream().findFirst();
    }

    @Override
    public List<Cuestionario> findLastModifiedLimit(int limit) {
        TypedQuery<Cuestionario> query = entityManager.createQuery(
            """
                SELECT c FROM Cuestionario c
                ORDER BY c.updatedAt DESC
            """, Cuestionario.class);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public Page<Cuestionario> findAll(QuizFilterForm filters, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Cuestionario> query = cb.createQuery(Cuestionario.class);
        Root<Cuestionario> root = query.from(Cuestionario.class);

        query.select(root);

        applySorting(cb, query, root, pageable);

        TypedQuery<Cuestionario> typedQuery = entityManager.createQuery(query);

        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults((int) pageable.getPageSize());

        List<Cuestionario> result = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Cuestionario> countRoot = countQuery.from(Cuestionario.class);

        countQuery.select(cb.count(countRoot));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(result, pageable, total);
    }

    private void applySorting(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Cuestionario> root, Pageable pageable){
        if (pageable.getSort().isUnsorted()) {
            query.orderBy(cb.desc(root.get("updatedAt")));
        }

        List<Order> orders = new ArrayList<>();
        
        Join<Cuestionario, Tema> tema = null;
        Join<Cuestionario, Dificultad> dificultad = null;
        Join<Cuestionario, Asignatura> asignatura = null;

        for (Sort.Order sort : pageable.getSort()) {
            Expression<?> expression;

            switch (sort.getProperty()) {
                case "titulo":
                    expression = root.get("titulo");
                    break;

                case "asignatura":
                    if (asignatura == null) {
                        tema = root.join("tema");
                        asignatura = tema.join("asignatura");
                    }

                    expression = asignatura.get("nombre");

                    break;
                
                case "tema":
                    if (tema == null) {
                        tema = root.join("tema");
                    }
                    
                    expression = tema.get("nombre");
                    break;

                case "dificultad":
                    if (dificultad == null) {
                        dificultad = root.join("dificultad");
                    }
                    
                    expression = dificultad.get("nombre");
                    break;

                case "version":
                    expression = root.get("version");
                    break;
                
                case "updatedAt":
                    expression = root.get("updatedAt");
                    break;

                default:
                    expression = root.get("updatedAt");
            }

            orders.add(sort.isAscending() ? cb.asc(expression) : cb.desc(expression));
        }

        query.orderBy(orders);
    }
}