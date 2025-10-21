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
public class DeviceModelRequestDto {

    @NotBlank
    @Size(max = 100)
    private String deviceName;

    @NotBlank
    @Size(max = 100)
    private String brand;

    @Size(max = 500)
    private String imageURL;

    @Size(max = 1000)
    private String specifications;

    private boolean isActive;

    @NotNull
    private Long deviceCategoryId;
}
