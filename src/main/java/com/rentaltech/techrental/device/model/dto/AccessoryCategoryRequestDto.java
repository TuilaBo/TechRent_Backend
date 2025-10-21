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
public class AccessoryCategoryRequestDto {

    @NotBlank
    @Size(max = 100)
    private String accessoryCategoryName;

    @Size(max = 500)
    private String description;

    private boolean isActive;
}
