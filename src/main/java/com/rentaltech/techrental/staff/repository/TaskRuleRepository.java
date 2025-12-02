package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.TaskRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRuleRepository extends JpaRepository<TaskRule, Long> {
    Optional<TaskRule> findFirstByActiveTrueOrderByEffectiveFromDesc();

    List<TaskRule> findByActive(Boolean active);

    @Query("""
            SELECT tr FROM TaskRule tr
            WHERE tr.active = true
              AND (tr.effectiveFrom IS NULL OR tr.effectiveFrom <= :now)
              AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :now)
            ORDER BY tr.effectiveFrom DESC
            """)
    Optional<TaskRule> findActiveRuleByCurrentDate(@Param("now") LocalDateTime now);

    default Optional<TaskRule> findActiveRuleByCurrentDate() {
        return findActiveRuleByCurrentDate(LocalDateTime.now());
    }
}

