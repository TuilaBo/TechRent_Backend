package com.rentaltech.techrental.device.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    @ManyToOne
    @JoinColumn(name = "brand_id", referencedColumnName = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "amount_available")
    private Long amountAvailable;

    @Column(name = "description", length = 300, nullable = false)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageURL;

    @Column(name = "specifications", length = 1000)
    private String specifications;

    @Column(name = "is_active")
    private boolean isActive;

    @ManyToOne
    @JoinColumn(name = "device_category_id", referencedColumnName = "device_category_id", nullable = false)
    private DeviceCategory deviceCategory;

    // Pricing fields for rental calculation
    @Column(name = "device_value", nullable = false)
    private BigDecimal deviceValue;

    @Column(name = "price_per_day", nullable = false)
    private BigDecimal pricePerDay;

    @Column(name = "deposit_percent", nullable = false)
    private BigDecimal depositPercent;
}
