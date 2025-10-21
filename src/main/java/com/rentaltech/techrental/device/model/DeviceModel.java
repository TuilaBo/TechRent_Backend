package com.rentaltech.techrental.device.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DeviceModel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceModel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "device_model_id", nullable = false)
    private Long deviceModelId;

    @Column(name = "deivce_name", length = 100, nullable = false)
    private String deviceName;

    @Column(name = "brand", length = 100, nullable = false)
    private String brand;

    @Column(name = "image_url", length = 500)
    private String imageURL;

    @Column(name = "specifications", length = 1000)
    private String specifications;

    @Column(name = "is_active")
    private boolean isActive;

    @ManyToOne
    @JoinColumn(name = "device_category_id", referencedColumnName = "device_category_id", nullable = false)
    private DeviceCategory deviceCategory;
}
