package com.rentaltech.techrental.device.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandRequestDto {
    @NotBlank
    @Size(max = 100)
    private String brandName;

    @Size(max = 500)
    private String description;

    private boolean isActive;
}

