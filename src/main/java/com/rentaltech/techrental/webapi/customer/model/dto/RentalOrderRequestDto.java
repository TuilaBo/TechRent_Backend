package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.OrderStatus;
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
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    @NotNull
    private OrderStatus orderStatus;

    @NotNull
    private Double depositAmount;

    @NotNull
    private Double depositAmountHeld;

    private Double depositAmountUsed;

    private Double depositAmountRefunded;

    @NotNull
    private Double totalPrice;

    @NotNull
    private Double pricePerDay;

    @NotNull
    private Long customerId;

    @NotNull
    @Size(min = 1)
    private List<@Valid OrderDetailRequestDto> orderDetails;
}
