package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.maintenance.model.MaintenancePlan;
import com.rentaltech.techrental.maintenance.model.MaintenancePlanScopeType;
import com.rentaltech.techrental.maintenance.model.MaintenanceRuleType;

import java.time.LocalDate;
import java.util.List;

public interface MaintenancePlanService {
    MaintenancePlan createPlan(String status,
                               Integer ruleValue,
                               MaintenancePlanScopeType scopeType,
                               MaintenanceRuleType ruleType,
                               Boolean active,
                               List<Long> deviceIds,
                               LocalDate startDate,
                               LocalDate endDate,
                               String scheduleStatus);

    List<MaintenancePlan> listPlans();
    
}



