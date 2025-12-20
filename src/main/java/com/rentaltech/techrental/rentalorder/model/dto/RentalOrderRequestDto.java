package com.rentaltech.techrental.rentalorder.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrderRequestDto {
    @NotNull
    private LocalDateTime planStartDate;

    @NotNull
    private LocalDateTime planEndDate;

    @NotNull
    private String shippingAddress;

    @NotNull
    @Size(min = 1)
    private List<@Valid OrderDetailRequestDto> orderDetails;
}
