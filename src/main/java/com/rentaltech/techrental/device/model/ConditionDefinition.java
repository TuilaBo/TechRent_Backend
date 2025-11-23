package com.rentaltech.techrental.device.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "condition_definition")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_definition_id")
    private Long conditionDefinitionId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_category_id")
    private DeviceCategory deviceCategory;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "impact_rate", precision = 5, scale = 2)
    private BigDecimal impactRate;

    @Column(name = "is_damage")
    private boolean damage;

    @Column(name = "default_compensation", precision = 19, scale = 2)
    private BigDecimal defaultCompensation;
}
