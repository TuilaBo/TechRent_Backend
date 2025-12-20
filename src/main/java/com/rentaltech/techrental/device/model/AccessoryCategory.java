/*package com.rentaltech.techrental.device.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AccessoryCategory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessoryCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "accessory_category_id", nullable = false)
    private Long accessoryCategoryId;

    @Column(name = "accessory_category_name", length = 100, nullable = false)
    private String accessoryCategoryName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active")
    private boolean isActive;
}
*/
