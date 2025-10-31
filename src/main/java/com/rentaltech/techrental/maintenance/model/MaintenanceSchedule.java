package com.rentaltech.techrental.maintenance.model;

import com.rentaltech.techrental.device.model.Device;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "MaintenanceSchedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "maintenance_schedule_id", nullable = false)
    private Long maintenanceScheduleId;

    @ManyToOne
    @JoinColumn(name = "maintenance_plan_id", referencedColumnName = "maintenance_plan_id")
    private MaintenancePlan maintenancePlan;

    @ManyToOne
    @JoinColumn(name = "device_id", referencedColumnName = "device_id")
    private Device device;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}


