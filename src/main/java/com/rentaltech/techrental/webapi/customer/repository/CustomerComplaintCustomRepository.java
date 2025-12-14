package com.rentaltech.techrental.webapi.customer.repository;

import com.rentaltech.techrental.webapi.customer.model.ComplaintStatus;
import com.rentaltech.techrental.webapi.customer.model.CustomerComplaint;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CustomerComplaintCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Tìm complaints theo customerId (qua rentalOrder.customer)
     */
    public List<CustomerComplaint> findByCustomerId(Long customerId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CustomerComplaint> query = cb.createQuery(CustomerComplaint.class);
        Root<CustomerComplaint> root = query.from(CustomerComplaint.class);

        // Join với rentalOrder và customer
        Join<?, ?> rentalOrderJoin = root.join("rentalOrder");
        Join<?, ?> customerJoin = rentalOrderJoin.join("customer");

        Predicate predicate = cb.equal(customerJoin.get("customerId"), customerId);
        query.where(predicate);

        TypedQuery<CustomerComplaint> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    /**
     * Tìm complaint theo orderId và deviceId
     */
    public Optional<CustomerComplaint> findByOrderIdAndDeviceId(Long orderId, Long deviceId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CustomerComplaint> query = cb.createQuery(CustomerComplaint.class);
        Root<CustomerComplaint> root = query.from(CustomerComplaint.class);

        List<Predicate> predicates = new ArrayList<>();

        // Join với rentalOrder
        Join<?, ?> rentalOrderJoin = root.join("rentalOrder");
        predicates.add(cb.equal(rentalOrderJoin.get("orderId"), orderId));

        // Join với device
        Join<?, ?> deviceJoin = root.join("device");
        predicates.add(cb.equal(deviceJoin.get("deviceId"), deviceId));

        query.where(cb.and(predicates.toArray(new Predicate[0])));

        TypedQuery<CustomerComplaint> typedQuery = entityManager.createQuery(query);
        List<CustomerComplaint> results = typedQuery.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Tìm complaints theo danh sách status, sắp xếp theo createdAt DESC
     */
    public List<CustomerComplaint> findByStatusInOrderByCreatedAtDesc(List<ComplaintStatus> statuses) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CustomerComplaint> query = cb.createQuery(CustomerComplaint.class);
        Root<CustomerComplaint> root = query.from(CustomerComplaint.class);

        if (statuses != null && !statuses.isEmpty()) {
            Predicate predicate = root.get("status").in(statuses);
            query.where(predicate);
        }

        // Order by createdAt DESC
        Order orderByCreatedAt = cb.desc(root.get("createdAt"));
        query.orderBy(orderByCreatedAt);

        TypedQuery<CustomerComplaint> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }
}

