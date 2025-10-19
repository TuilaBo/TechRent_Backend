package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TaskCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Task> findOverdueTasks(LocalDateTime currentDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Task> query = cb.createQuery(Task.class);
        Root<Task> task = query.from(Task.class);

        // Tạo predicates
        Predicate plannedEndBeforeNow = cb.lessThan(task.get("plannedEnd"), currentDate);
        Predicate statusNotCompleted = cb.notEqual(task.get("status"), TaskStatus.COMPLETED);

        // Kết hợp predicates với AND
        Predicate overdueCondition = cb.and(plannedEndBeforeNow, statusNotCompleted);

        query.select(task).where(overdueCondition);

        return entityManager.createQuery(query).getResultList();
    }

    public List<Task> findTasksByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Task> query = cb.createQuery(Task.class);
        Root<Task> task = query.from(Task.class);

        // Tạo predicates
        Predicate plannedStartAfterStartDate = cb.greaterThanOrEqualTo(task.get("plannedStart"), startDate);
        Predicate plannedStartBeforeEndDate = cb.lessThanOrEqualTo(task.get("plannedStart"), endDate);

        // Kết hợp predicates với AND
        Predicate dateRangeCondition = cb.and(plannedStartAfterStartDate, plannedStartBeforeEndDate);

        query.select(task).where(dateRangeCondition);

        return entityManager.createQuery(query).getResultList();
    }
}
