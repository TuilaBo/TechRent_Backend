package com.rentaltech.techrental.device.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discrepancy_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discrepancy_report_id")
    private Long discrepancyReportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_from", nullable = false, length = 50)
    private DiscrepancyCreatedFrom createdFrom;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false, length = 50)
    private DiscrepancyType discrepancyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_definition_id")
    private ConditionDefinition conditionDefinition;

    @Column(name = "penalty_amount", precision = 19, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "staff_note", columnDefinition = "text")
    private String staffNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
