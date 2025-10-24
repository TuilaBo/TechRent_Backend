package com.rentaltech.techrental.device.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCategoryRequestDto {

    @NotBlank
    @Size(max = 100)
    private String deviceCategoryName;

    @Size(max = 500)
    private String description;

    private boolean isActive;
}
