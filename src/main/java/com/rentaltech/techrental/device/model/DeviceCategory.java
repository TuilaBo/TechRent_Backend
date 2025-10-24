package com.rentaltech.techrental.device.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DeviceCategory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "device_category_id", nullable = false)
    private Long deviceCategoryId;

    @Column(name = "device_category_name", length = 100, nullable = false)
    private String deviceCategoryName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active")
    private boolean isActive;
}
