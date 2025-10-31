package com.rentaltech.techrental.maintenance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "MaintenancePlan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenancePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "maintenance_plan_id", nullable = false)
    private Long maintenancePlanId;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "rule_value")
    private Integer ruleValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", length = 50)
    private MaintenancePlanScopeType scopeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", length = 50)
    private MaintenanceRuleType ruleType;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "last_generated_at")
    private LocalDateTime lastGeneratedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}


