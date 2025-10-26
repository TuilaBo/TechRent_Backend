// java
package com.rentaltech.techrental.device.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Device")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "serial_number", unique = true, length = 100)
    private String serialNumber;

    @Column(name = "acquire_at")
    private LocalDateTime acquireAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeviceStatus status;

//    @Column(name = "shelf_code", length = 100)
//    private String shelfCode;

    @ManyToOne
    @JoinColumn(name = "device_model_id", referencedColumnName = "device_model_id", nullable = false)
    private DeviceModel deviceModel;
}
