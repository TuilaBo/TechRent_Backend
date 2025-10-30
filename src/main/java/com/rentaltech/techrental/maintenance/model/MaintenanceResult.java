package com.rentaltech.techrental.maintenance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "MaintenanceResult")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceResult {

    @EmbeddedId
    private MaintenanceResultId id;

    @ManyToOne(optional = false)
    @MapsId("maintenanceItemId")
    @JoinColumn(name = "maintenance_item_id", referencedColumnName = "maintenance_item_id", nullable = false)
    private MaintenanceItem maintenanceItem;

    @ManyToOne(optional = false)
    @MapsId("maintenanceScheduleId")
    @JoinColumn(name = "maintenance_schedule_id", referencedColumnName = "maintenance_schedule_id", nullable = false)
    private MaintenanceSchedule maintenanceSchedule;

    @Column(name = "result")
    private String result;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "complete_at")
    private LocalDateTime completeAt;
}


