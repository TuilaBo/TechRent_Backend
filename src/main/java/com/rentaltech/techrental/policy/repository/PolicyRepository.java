package com.rentaltech.techrental.policy.repository;

import com.rentaltech.techrental.policy.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByDeletedAtIsNullOrderByCreatedAtDesc();

    @Query("""
            SELECT p FROM Policy p
            WHERE p.deletedAt IS NULL
              AND (p.effectiveFrom IS NULL OR p.effectiveFrom <= :date)
              AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date)
            ORDER BY p.effectiveFrom DESC, p.createdAt DESC
            """)
    List<Policy> findActivePoliciesByDate(@Param("date") LocalDate date);

    default List<Policy> findActivePolicies() {
        return findActivePoliciesByDate(LocalDate.now());
    }

    Optional<Policy> findByPolicyIdAndDeletedAtIsNull(Long policyId);
}

