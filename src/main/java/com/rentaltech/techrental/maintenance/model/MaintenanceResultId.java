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
public class MaintenanceResultId implements Serializable {

    @Column(name = "maintenance_item_id")
    private Long maintenanceItemId;

    @Column(name = "maintenance_schedule_id")
    private Long maintenanceScheduleId;
}


