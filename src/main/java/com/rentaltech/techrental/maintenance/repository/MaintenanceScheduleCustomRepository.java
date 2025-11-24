package com.rentaltech.techrental.maintenance.repository;

import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
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

    public List<MaintenanceSchedule> findConflictingSchedules(
            List<Long> deviceIds,
            LocalDate startDate,
            LocalDate endDate) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MaintenanceSchedule> query = cb.createQuery(MaintenanceSchedule.class);
        Root<MaintenanceSchedule> schedule = query.from(MaintenanceSchedule.class);

        List<Predicate> predicates = new ArrayList<>();

        // s.device.deviceId IN :deviceIds
        if (deviceIds != null && !deviceIds.isEmpty()) {
            predicates.add(schedule.get("device").get("deviceId").in(deviceIds));
        }

        // s.startDate <= :endDate
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(schedule.get("startDate"), endDate));
        }

        // s.endDate >= :startDate
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(schedule.get("endDate"), startDate));
        }

        query.select(schedule).where(cb.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(query).getResultList();
    }

    public List<MaintenanceSchedule> findActiveMaintenanceSchedules() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MaintenanceSchedule> query = cb.createQuery(MaintenanceSchedule.class);
        Root<MaintenanceSchedule> schedule = query.from(MaintenanceSchedule.class);

        LocalDate today = LocalDate.now();
        List<Predicate> predicates = new ArrayList<>();

        // Status is IN_PROGRESS or STARTED (case-insensitive, handle null)
        Predicate statusInProgress = cb.or(
                cb.equal(cb.upper(cb.coalesce(schedule.get("status"), cb.literal(""))), "IN_PROGRESS"),
                cb.equal(cb.upper(cb.coalesce(schedule.get("status"), cb.literal(""))), "STARTED")
        );

        // OR: Currently within maintenance period (startDate <= today <= endDate) 
        // AND status is not completed/cancelled
        Predicate withinPeriod = cb.and(
                cb.lessThanOrEqualTo(schedule.get("startDate"), today),
                cb.greaterThanOrEqualTo(schedule.get("endDate"), today),
                cb.or(
                        schedule.get("status").isNull(),
                        cb.not(cb.upper(schedule.get("status")).in("COMPLETED", "FINISHED", "CANCELLED", "CANCELED"))
                )
        );

        predicates.add(cb.or(statusInProgress, withinPeriod));

        query.select(schedule).where(cb.and(predicates.toArray(new Predicate[0])));
        query.orderBy(cb.asc(schedule.get("startDate")));

        return entityManager.createQuery(query).getResultList();
    }
}


