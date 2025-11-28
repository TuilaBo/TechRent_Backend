package com.rentaltech.techrental.contract.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeviceContractTermRequestDto {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    private Long deviceId;

    private Long deviceCategoryId;

    private Boolean active = Boolean.TRUE;
}

