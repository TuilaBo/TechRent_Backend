package com.rentaltech.techrental.rentalorder.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrderExtendRequestDto {

    @NotNull
    private Long rentalOrderId;

    @NotNull
    private LocalDateTime extendedEndTime;
}
