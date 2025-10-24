package com.rentaltech.techrental.device.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Accessory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accessory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "accessory_id", nullable = false)
    private Long accessoryId;

    @Column(name = "accessory_name", length = 100, nullable = false)
    private String accessoryName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active")
    private boolean isActive;

    @ManyToOne
    @JoinColumn(name = "accessory_category_id", referencedColumnName = "accessory_category_id", nullable = false)
    private AccessoryCategory accessoryCategory;

    @ManyToOne
    @JoinColumn(name = "device_model_id", referencedColumnName = "device_model_id")
    private DeviceModel deviceModel;
}
