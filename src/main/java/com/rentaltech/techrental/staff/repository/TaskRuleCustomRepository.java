package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.model.TaskRule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskRuleCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<TaskRule> findActiveRuleByContext(LocalDateTime now, StaffRole role, TaskCategoryType taskCategory) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TaskRule> query = cb.createQuery(TaskRule.class);
        Root<TaskRule> root = query.from(TaskRule.class);

        List<Predicate> predicates = new ArrayList<>();

        // active = true
        predicates.add(cb.isTrue(root.get("active")));

        // effectiveFrom <= now OR null
        predicates.add(cb.or(
                root.get("effectiveFrom").isNull(),
                cb.lessThanOrEqualTo(root.get("effectiveFrom"), now)
        ));

        // effectiveTo >= now OR null
        predicates.add(cb.or(
                root.get("effectiveTo").isNull(),
                cb.greaterThanOrEqualTo(root.get("effectiveTo"), now)
        ));

        // (:role is null OR staffRole is null OR staffRole = :role)
        if (role != null) {
            predicates.add(cb.or(
                    root.get("staffRole").isNull(),
                    cb.equal(root.get("staffRole"), role)
            ));
        }

        // (:taskCategory is null OR taskCategory = :taskCategory)
        if (taskCategory != null) {
            predicates.add(cb.or(
                    root.get("taskCategory").isNull(),
                    cb.equal(root.get("taskCategory"), taskCategory)
            ));
        }

        // Priority ordering using CASE expression
        Expression<Integer> priority = cb.<Integer>selectCase()
                .when(
                        cb.and(
                                roleEqual(cb, root, role),
                                categoryEqual(cb, root, taskCategory)
                        ), 0)
                .when(
                        cb.and(
                                roleEqual(cb, root, role),
                                root.get("taskCategory").isNull()
                        ), 1)
                .when(
                        cb.and(
                                root.get("staffRole").isNull(),
                                categoryEqual(cb, root, taskCategory)
                        ), 2)
                .otherwise(3);

        query.select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(
                        cb.asc(priority),
                        cb.desc(root.get("effectiveFrom"))
                );

        List<TaskRule> results = entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultList();

        return results.stream().findFirst();
    }

    public List<TaskRule> findAllActiveRules(LocalDateTime now) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TaskRule> query = cb.createQuery(TaskRule.class);
        Root<TaskRule> root = query.from(TaskRule.class);

        List<Predicate> predicates = new ArrayList<>();

        // active = true
        predicates.add(cb.isTrue(root.get("active")));

        // effectiveFrom <= now OR null
        predicates.add(cb.or(
                root.get("effectiveFrom").isNull(),
                cb.lessThanOrEqualTo(root.get("effectiveFrom"), now)
        ));

        // effectiveTo >= now OR null
        predicates.add(cb.or(
                root.get("effectiveTo").isNull(),
                cb.greaterThanOrEqualTo(root.get("effectiveTo"), now)
        ));

        query.select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(root.get("effectiveFrom")));

        return entityManager.createQuery(query).getResultList();
    }

    private Predicate roleEqual(CriteriaBuilder cb, Root<TaskRule> root, StaffRole role) {
        if (role == null) {
            // always false, we only use this helper inside CASE when role != null
            return cb.disjunction();
        }
        return cb.equal(root.get("staffRole"), role);
    }

    private Predicate categoryEqual(CriteriaBuilder cb, Root<TaskRule> root, TaskCategoryType taskCategory) {
        if (taskCategory == null) {
            // always false, we only use this helper inside CASE when categoryId != null
            return cb.disjunction();
        }
        return cb.equal(root.get("taskCategory"), taskCategory);
    }
}


