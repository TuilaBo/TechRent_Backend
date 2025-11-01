package com.rentaltech.techrental.maintenance.model;

import com.rentaltech.techrental.device.model.Device;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MaintenancePlanDevice")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenancePlanDevice {

    @EmbeddedId
    private MaintenancePlanDeviceId id;

    @ManyToOne(optional = false)
    @MapsId("maintenancePlanId")
    @JoinColumn(name = "maintenance_plan_id", referencedColumnName = "maintenance_plan_id", nullable = false)
    private MaintenancePlan maintenancePlan;

    @ManyToOne(optional = false)
    @MapsId("deviceId")
    @JoinColumn(name = "device_id", referencedColumnName = "device_id", nullable = false)
    private Device device;
}




