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
public class MaintenancePlanDeviceId implements Serializable {

    @Column(name = "maintenance_plan_id")
    private Long maintenancePlanId;

    @Column(name = "device_id")
    private Long deviceId;
}



