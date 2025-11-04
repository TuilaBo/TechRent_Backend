package com.rentaltech.techrental.device.model.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceModelRequestDto {

    @NotBlank
    @Size(max = 100)
    private String deviceName;

    @NotNull
    private Long brandId;

    @Size(max = 500)
    private String imageURL;

    @Size(max = 1000)
    private String specifications;

    @NotBlank
    @Size(max = 300)
    private String description;

    private boolean isActive;

    @NotNull
    private Long deviceCategoryId;

    // Pricing fields
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal deviceValue;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal pricePerDay;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal depositPercent;
}
