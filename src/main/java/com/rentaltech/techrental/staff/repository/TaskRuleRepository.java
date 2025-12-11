package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.model.TaskRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRuleRepository extends JpaRepository<TaskRule, Long> {
    Optional<TaskRule> findFirstByActiveTrueOrderByEffectiveFromDesc();

    List<TaskRule> findByActive(Boolean active);

    Optional<TaskRule> findByTaskCategory(TaskCategoryType taskCategory);
}
