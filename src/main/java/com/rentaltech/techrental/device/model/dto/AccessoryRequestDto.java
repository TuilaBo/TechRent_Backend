package com.rentaltech.techrental.device.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessoryRequestDto {

    @NotBlank
    @Size(max = 100)
    private String accessoryName;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String imageUrl;

    private boolean isActive;

    @NotNull
    private Long accessoryCategoryId;

    private Long deviceModelId;
}
