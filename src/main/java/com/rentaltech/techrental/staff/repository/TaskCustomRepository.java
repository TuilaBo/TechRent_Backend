package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.StaffTaskCompletionStatsDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        var accountJoin = staffJoin.join("account");
        
        query.multiselect(
                staffJoin.get("staffId"),
                cb.coalesce(accountJoin.get("username"), ""),
                categoryJoin.get("taskCategoryId"),
                categoryJoin.get("name"),
                cb.countDistinct(task)
        )
        .where(basePredicate)
        .groupBy(
                staffJoin.get("staffId"),
                accountJoin.get("username"),
                categoryJoin.get("taskCategoryId"),
                categoryJoin.get("name")
        )
        .orderBy(cb.asc(categoryJoin.get("name")));

        return entityManager.createQuery(query).getResultList();
    }

    public Page<StaffTaskCompletionStatsDto> findStaffCompletionsByPeriod(
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            StaffRole staffRole, 
            Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Query để lấy data
        CriteriaQuery<StaffTaskCompletionStatsDto> dataQuery = cb.createQuery(StaffTaskCompletionStatsDto.class);
        Root<Task> task = dataQuery.from(Task.class);
        Join<?, ?> staffJoin = task.join("assignedStaff");
        Join<?, ?> accountJoin = staffJoin.join("account");
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(task.get("status"), TaskStatus.COMPLETED));
        predicates.add(cb.between(task.get("completedAt"), startTime, endTime));
        
        if (staffRole != null) {
            predicates.add(cb.equal(staffJoin.get("staffRole"), staffRole));
        }
        
        Predicate whereClause = cb.and(predicates.toArray(new Predicate[0]));
        
        // Select với constructor
        dataQuery.select(cb.construct(
                StaffTaskCompletionStatsDto.class,
                staffJoin.get("staffId"),
                accountJoin.get("accountId"),
                accountJoin.get("username"),
                accountJoin.get("email"),
                accountJoin.get("phoneNumber"),
                staffJoin.get("staffRole"),
                cb.count(task)
        ));
        
        dataQuery.where(whereClause);
        dataQuery.groupBy(
                staffJoin.get("staffId"),
                accountJoin.get("accountId"),
                accountJoin.get("username"),
                accountJoin.get("email"),
                accountJoin.get("phoneNumber"),
                staffJoin.get("staffRole")
        );
        
        // Apply sorting - Note: Can't sort by aggregate in GROUP BY query directly
        // We'll apply default sort by completedTaskCount desc, other sorts need to be handled differently
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(sortOrder -> {
                String property = sortOrder.getProperty();
                if ("completedTaskCount".equals(property)) {
                    // Sort by count - use aggregate function
                    Expression<Long> countExpr = cb.count(task);
                    Order order = sortOrder.isAscending() 
                            ? cb.asc(countExpr) 
                            : cb.desc(countExpr);
                    orders.add(order);
                } else {
                    // Sort by other fields from group by
                    Expression<?> expr = getExpressionForProperty(staffJoin, accountJoin, property);
                    if (expr != null) {
                        Order order = sortOrder.isAscending() 
                                ? cb.asc(expr) 
                                : cb.desc(expr);
                        orders.add(order);
                    }
                }
            });
            if (!orders.isEmpty()) {
                dataQuery.orderBy(orders);
            } else {
                // Default sort by completedTaskCount desc
                dataQuery.orderBy(cb.desc(cb.count(task)));
            }
        } else {
            // Default sort by completedTaskCount desc
            dataQuery.orderBy(cb.desc(cb.count(task)));
        }
        
        TypedQuery<StaffTaskCompletionStatsDto> typedQuery = entityManager.createQuery(dataQuery);
        
        // Apply pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<StaffTaskCompletionStatsDto> content = typedQuery.getResultList();
        
        // Count query
        long total = countStaffCompletionsByPeriod(startTime, endTime, staffRole);
        
        return new PageImpl<>(content, pageable, total);
    }
    
    public long countStaffCompletionsByPeriod(
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            StaffRole staffRole) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Task> task = countQuery.from(Task.class);
        Join<?, ?> staffJoin = task.join("assignedStaff");
        
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(task.get("status"), TaskStatus.COMPLETED));
        predicates.add(cb.between(task.get("completedAt"), startTime, endTime));
        
        if (staffRole != null) {
            predicates.add(cb.equal(staffJoin.get("staffRole"), staffRole));
        }
        
        countQuery.select(cb.countDistinct(staffJoin.get("staffId")));
        countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        
        return entityManager.createQuery(countQuery).getSingleResult();
    }
    
    private Expression<?> getExpressionForProperty(Join<?, ?> staffJoin, Join<?, ?> accountJoin, String property) {
        return switch (property) {
            case "staffId" -> staffJoin.get("staffId");
            case "accountId", "username", "email", "phoneNumber" -> accountJoin.get(property);
            case "staffRole" -> staffJoin.get("staffRole");
            default -> null;
        };
    }

    public Page<Task> findTasksWithFilters(
            Long categoryId,
            Long orderId,
            Long assignedStaffId,
            TaskStatus status,
            Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Data query
        CriteriaQuery<Task> dataQuery = cb.createQuery(Task.class);
        Root<Task> task = dataQuery.from(Task.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (categoryId != null) {
            predicates.add(cb.equal(task.get("taskCategory").get("taskCategoryId"), categoryId));
        }
        
        if (orderId != null) {
            predicates.add(cb.equal(task.get("orderId"), orderId));
        }
        
        if (assignedStaffId != null) {
            Join<?, ?> staffJoin = task.join("assignedStaff");
            predicates.add(cb.equal(staffJoin.get("staffId"), assignedStaffId));
        }
        
        if (status != null) {
            predicates.add(cb.equal(task.get("status"), status));
        }
        
        if (!predicates.isEmpty()) {
            dataQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        
        // Sort theo plannedEnd ASC trước (sẽ được sort lại trong Java theo khoảng cách tuyệt đối)
        // Tạm thời sort theo plannedEnd để có thứ tự ban đầu
        Expression<LocalDateTime> plannedEndExpr = task.get("plannedEnd");
        LocalDateTime farFuture = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
        Expression<LocalDateTime> sortExpr = cb.coalesce(plannedEndExpr, farFuture);
        Order orderByPlannedEnd = cb.asc(sortExpr);
        // Secondary sort: createdAt DESC
        dataQuery.orderBy(orderByPlannedEnd, cb.desc(task.get("createdAt")));
        
        TypedQuery<Task> typedQuery = entityManager.createQuery(dataQuery);
        
        // Apply pagination (chỉ khi không phải unpaged)
        List<Task> content;
        long total = countTasksWithFilters(categoryId, orderId, assignedStaffId, status);
        
        if (pageable.isUnpaged()) {
            // Không apply pagination, lấy tất cả
            content = typedQuery.getResultList();
        } else {
            // Apply pagination
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
            content = typedQuery.getResultList();
        }
        
        return new PageImpl<>(content, pageable, total);
    }
    
    private long countTasksWithFilters(
            Long categoryId,
            Long orderId,
            Long assignedStaffId,
            TaskStatus status) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Task> task = countQuery.from(Task.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (categoryId != null) {
            predicates.add(cb.equal(task.get("taskCategory").get("taskCategoryId"), categoryId));
        }
        
        if (orderId != null) {
            predicates.add(cb.equal(task.get("orderId"), orderId));
        }
        
        if (assignedStaffId != null) {
            Join<?, ?> staffJoin = task.join("assignedStaff");
            predicates.add(cb.equal(staffJoin.get("staffId"), assignedStaffId));
        }
        
        if (status != null) {
            predicates.add(cb.equal(task.get("status"), status));
        }
        
        countQuery.select(cb.countDistinct(task));
        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
