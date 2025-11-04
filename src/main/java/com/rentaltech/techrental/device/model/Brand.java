package com.rentaltech.techrental.device.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Brand")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "brand_name", length = 100, nullable = false)
    private String brandName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active")
    private boolean isActive;
}

