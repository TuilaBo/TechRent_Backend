package com.rentaltech.techrental.maintenance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceConflictId implements Serializable {

    @Column(name = "maintenance_schedule_id")
    private Long maintenanceScheduleId;

    @Column(name = "booking_calendar_id")
    private Long bookingCalendarId;
}


