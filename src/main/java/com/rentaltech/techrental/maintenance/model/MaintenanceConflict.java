package com.rentaltech.techrental.maintenance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "MaintenanceConflict")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceConflict {

    @EmbeddedId
    private MaintenanceConflictId id;

    @ManyToOne(optional = false)
    @MapsId("maintenanceScheduleId")
    @JoinColumn(name = "maintenance_schedule_id", referencedColumnName = "maintenance_schedule_id", nullable = false)
    private MaintenanceSchedule maintenanceSchedule;

    // BookingCalendar entity not present in codebase; keep scalar id for now (read-only to avoid duplicate mapping with EmbeddedId)
    @Column(name = "booking_calendar_id", nullable = false, insertable = false, updatable = false)
    private Long bookingCalendarId;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "status", length = 50)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 50)
    private MaintenanceConflictSource source;
}


