package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.BookingCalendar;
import com.rentaltech.techrental.rentalorder.model.BookingStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class BookingCalendarRepositoryCustomImpl implements BookingCalendarRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countOverlappingByModel(Long deviceModelId,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime,
                                        Collection<BookingStatus> statuses) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<BookingCalendar> root = query.from(BookingCalendar.class);

        List<Predicate> predicates = buildCommonPredicates(cb, root, deviceModelId, startTime, endTime, statuses);

        query.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));
        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public Set<Long> findBusyDeviceIdsByModelAndRange(Long deviceModelId,
                                                      LocalDateTime startTime,
                                                      LocalDateTime endTime,
                                                      Collection<BookingStatus> statuses) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<BookingCalendar> root = query.from(BookingCalendar.class);

        List<Predicate> predicates = buildCommonPredicates(cb, root, deviceModelId, startTime, endTime, statuses);

        query.select(root.get("device").get("deviceId"))
                .distinct(true)
                .where(cb.and(predicates.toArray(new Predicate[0])));

        return new HashSet<>(entityManager.createQuery(query).getResultList());
    }

    @Override
    public List<BookingCalendar> findUpcomingBookings(LocalDateTime start,
                                                      LocalDateTime end,
                                                      Collection<BookingStatus> statuses) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BookingCalendar> query = cb.createQuery(BookingCalendar.class);
        Root<BookingCalendar> root = query.from(BookingCalendar.class);

        List<Predicate> predicates = new ArrayList<>();
        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(root.get("status").in(statuses));
        }
        if (start != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), start));
        }
        if (end != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), end));
        }

        query.select(root)
                .where(cb.and(predicates.toArray(new Predicate[0])))
                .orderBy(cb.asc(root.get("startTime")));

        return entityManager.createQuery(query).getResultList();
    }

    private List<Predicate> buildCommonPredicates(CriteriaBuilder cb,
                                                  Root<BookingCalendar> root,
                                                  Long deviceModelId,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  Collection<BookingStatus> statuses) {
        List<Predicate> predicates = new ArrayList<>();
        if (deviceModelId != null) {
            predicates.add(cb.equal(root.get("device").get("deviceModel").get("deviceModelId"), deviceModelId));
        }
        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(root.get("status").in(statuses));
        }
        if (endTime != null) {
            predicates.add(cb.lessThan(root.get("startTime"), endTime));
        }
        if (startTime != null) {
            predicates.add(cb.greaterThan(root.get("endTime"), startTime));
        }
        return predicates;
    }
}


