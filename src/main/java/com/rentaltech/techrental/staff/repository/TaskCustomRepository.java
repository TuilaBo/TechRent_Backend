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

import java.time.LocalDate;
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

    public long countActiveTasksByStaffAndDate(Long staffId, LocalDate targetDate, Long excludeTaskId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Task> task = query.from(Task.class);

        var staffJoin = task.join("assignedStaff");

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Predicate staffMatch = cb.equal(staffJoin.get("staffId"), staffId);

        Predicate statusPending = cb.equal(task.get("status"), TaskStatus.PENDING);
        Predicate statusInProgress = cb.equal(task.get("status"), TaskStatus.IN_PROGRESS);
        Predicate statusCompleted = cb.equal(task.get("status"), TaskStatus.COMPLETED);
        Predicate activeStatus = cb.or(statusPending, statusInProgress, statusCompleted);

        Predicate plannedInDay = cb.and(
                cb.isNotNull(task.get("plannedStart")),
                cb.greaterThanOrEqualTo(task.get("plannedStart"), startOfDay),
                cb.lessThan(task.get("plannedStart"), endOfDay)
        );

        Predicate createdInDay = cb.and(
                cb.isNull(task.get("plannedStart")),
                cb.greaterThanOrEqualTo(task.get("createdAt"), startOfDay),
                cb.lessThan(task.get("createdAt"), endOfDay)
        );

        Predicate dateMatch = cb.or(plannedInDay, createdInDay);

        Predicate basePredicate = cb.and(staffMatch, activeStatus, dateMatch);

        if (excludeTaskId != null) {
            Predicate notSameTask = cb.notEqual(task.get("taskId"), excludeTaskId);
            basePredicate = cb.and(basePredicate, notSameTask);
        }

        query.select(cb.countDistinct(task)).where(basePredicate);

        return entityManager.createQuery(query).getSingleResult();
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

    public long countActiveTasksByStaffCategoryAndDate(Long staffId, Long categoryId, LocalDate targetDate, Long excludeTaskId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Task> task = query.from(Task.class);

        var staffJoin = task.join("assignedStaff");

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Predicate staffMatch = cb.equal(staffJoin.get("staffId"), staffId);
        Predicate categoryMatch = cb.equal(task.get("taskCategory").get("taskCategoryId"), categoryId);

        Predicate statusPending = cb.equal(task.get("status"), TaskStatus.PENDING);
        Predicate statusInProgress = cb.equal(task.get("status"), TaskStatus.IN_PROGRESS);
        Predicate statusCompleted = cb.equal(task.get("status"), TaskStatus.COMPLETED);
        Predicate activeStatus = cb.or(statusPending, statusInProgress, statusCompleted);

        Predicate plannedInDay = cb.and(
                cb.isNotNull(task.get("plannedStart")),
                cb.greaterThanOrEqualTo(task.get("plannedStart"), startOfDay),
                cb.lessThan(task.get("plannedStart"), endOfDay)
        );

        Predicate createdInDay = cb.and(
                cb.isNull(task.get("plannedStart")),
                cb.greaterThanOrEqualTo(task.get("createdAt"), startOfDay),
                cb.lessThan(task.get("createdAt"), endOfDay)
        );

        Predicate dateMatch = cb.or(plannedInDay, createdInDay);

        Predicate basePredicate = cb.and(staffMatch, categoryMatch, activeStatus, dateMatch);

        if (excludeTaskId != null) {
            Predicate notSameTask = cb.notEqual(task.get("taskId"), excludeTaskId);
            basePredicate = cb.and(basePredicate, notSameTask);
        }

        query.select(cb.countDistinct(task)).where(basePredicate);

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<Object[]> countActiveTasksByStaffCategoryAndDateGrouped(Long staffId, LocalDate targetDate, Long categoryId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Task> task = query.from(Task.class);

        var staffJoin = task.join("assignedStaff");
        var categoryJoin = task.join("taskCategory");

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Predicate staffMatch = cb.equal(staffJoin.get("staffId"), staffId);

        Predicate statusPending = cb.equal(task.get("status"), TaskStatus.PENDING);
        Predicate statusInProgress = cb.equal(task.get("status"), TaskStatus.IN_PROGRESS);
        Predicate statusCompleted = cb.equal(task.get("status"), TaskStatus.COMPLETED);
        Predicate activeStatus = cb.or(statusPending, statusInProgress, statusCompleted);

        Predicate plannedInDay = cb.and(
                cb.isNotNull(task.get("plannedStart")),
                cb.greaterThanOrEqualTo(task.get("plannedStart"), startOfDay),
                cb.lessThan(task.get("plannedStart"), endOfDay)
        );

        Predicate createdInDay = cb.and(
                cb.isNull(task.get("plannedStart")),
                cb.greaterThanOrEqualTo(task.get("createdAt"), startOfDay),
                cb.lessThan(task.get("createdAt"), endOfDay)
        );

        Predicate dateMatch = cb.or(plannedInDay, createdInDay);

        Predicate basePredicate = cb.and(staffMatch, activeStatus, dateMatch);

        if (categoryId != null) {
            Predicate categoryMatch = cb.equal(categoryJoin.get("taskCategoryId"), categoryId);
            basePredicate = cb.and(basePredicate, categoryMatch);
        }

        query.multiselect(
                staffJoin.get("staffId"),
                cb.concat(cb.concat(
                        cb.coalesce(staffJoin.get("account").get("firstName"), ""), 
                        " "), 
                        cb.coalesce(staffJoin.get("account").get("lastName"), "")),
                categoryJoin.get("taskCategoryId"),
                categoryJoin.get("name"),
                cb.countDistinct(task)
        )
        .where(basePredicate)
        .groupBy(
                staffJoin.get("staffId"),
                staffJoin.get("account").get("firstName"),
                staffJoin.get("account").get("lastName"),
                categoryJoin.get("taskCategoryId"),
                categoryJoin.get("name")
        )
        .orderBy(cb.asc(categoryJoin.get("name")));

        return entityManager.createQuery(query).getResultList();
    }
}
