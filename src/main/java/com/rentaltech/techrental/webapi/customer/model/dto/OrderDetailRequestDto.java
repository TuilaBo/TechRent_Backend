package com.rentaltech.techrental.webapi.customer.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailRequestDto {
    @NotNull
    private Long quantity;

    @NotNull
    private Long deviceModelId;
}
