package com.rentaltech.techrental.maintenance.model;

import com.rentaltech.techrental.device.model.Device;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "MaintenanceItem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "maintenance_item_id", nullable = false)
    private Long maintenanceItemId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "device_id", referencedColumnName = "device_id", nullable = false)
    private Device device;

    @Column(name = "description")
    private String description;

    @Column(name = "estimate_time")
    private Integer estimateTime;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "outcome_note")
    private String outcomeNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}


