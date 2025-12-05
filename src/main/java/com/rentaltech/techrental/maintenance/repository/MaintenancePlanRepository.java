package com.rentaltech.techrental.maintenance.repository;

import com.rentaltech.techrental.maintenance.model.MaintenancePlan;
import com.rentaltech.techrental.maintenance.model.MaintenanceRuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, Long> {
    List<MaintenancePlan> findByRuleTypeAndActive(MaintenanceRuleType ruleType, Boolean active);
}


