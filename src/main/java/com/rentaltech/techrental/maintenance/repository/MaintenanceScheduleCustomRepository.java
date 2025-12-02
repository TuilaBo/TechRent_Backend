package com.rentaltech.techrental.maintenance.repository;

import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.model.MaintenanceScheduleStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MaintenanceScheduleCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<MaintenanceSchedule> findActiveMaintenanceSchedules() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MaintenanceSchedule> query = cb.createQuery(MaintenanceSchedule.class);
        Root<MaintenanceSchedule> schedule = query.from(MaintenanceSchedule.class);

        LocalDate today = LocalDate.now();
        List<Predicate> predicates = new ArrayList<>();

        // Status is IN_PROGRESS or STARTED (case-insensitive, handle null)
        Predicate statusInProgress = cb.or(
                cb.equal(cb.upper(cb.coalesce(schedule.get("status"), cb.literal(""))), MaintenanceScheduleStatus.IN_PROGRESS.name()),
                cb.equal(cb.upper(cb.coalesce(schedule.get("status"), cb.literal(""))), MaintenanceScheduleStatus.STARTED.name())
        );

        // OR: Currently within maintenance period (startDate <= today <= endDate) 
        // AND status is not completed/cancelled
        Predicate withinPeriod = cb.and(
                cb.lessThanOrEqualTo(schedule.get("startDate"), today),
                cb.greaterThanOrEqualTo(schedule.get("endDate"), today),
                cb.or(
                        schedule.get("status").isNull(),
                        cb.not(cb.upper(schedule.get("status")).in(
                                MaintenanceScheduleStatus.COMPLETED.name(),
                                MaintenanceScheduleStatus.FINISHED.name(),
                                MaintenanceScheduleStatus.CANCELLED.name(),
                                MaintenanceScheduleStatus.CANCELED.name()))
                )
        );

        predicates.add(cb.or(statusInProgress, withinPeriod));

        query.select(schedule).where(cb.and(predicates.toArray(new Predicate[0])));
        query.orderBy(cb.asc(schedule.get("startDate")));

        return entityManager.createQuery(query).getResultList();
    }

    public List<MaintenanceSchedule> findInactiveMaintenanceSchedules() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MaintenanceSchedule> query = cb.createQuery(MaintenanceSchedule.class);
        Root<MaintenanceSchedule> schedule = query.from(MaintenanceSchedule.class);

        LocalDate today = LocalDate.now();

        List<String> inactiveStatuses = List.of(
                MaintenanceScheduleStatus.COMPLETED.name(),
                MaintenanceScheduleStatus.FINISHED.name(),
                MaintenanceScheduleStatus.CANCELLED.name(),
                MaintenanceScheduleStatus.CANCELED.name(),
                MaintenanceScheduleStatus.FAILED.name()
        );

        // status thuộc nhóm inactive
        Predicate statusInactive = cb.upper(cb.coalesce(schedule.get("status"), cb.literal("")))
                .in(inactiveStatuses);

        // hoặc đã kết thúc trước ngày hôm nay
        Predicate endedBeforeToday = cb.and(
                schedule.get("endDate").isNotNull(),
                cb.lessThan(schedule.get("endDate"), today)
        );

        query.select(schedule)
                .where(cb.or(statusInactive, endedBeforeToday))
                .orderBy(cb.desc(schedule.get("startDate")));

        return entityManager.createQuery(query).getResultList();
    }
}


