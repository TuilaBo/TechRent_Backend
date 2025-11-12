package com.rentaltech.techrental.staff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverPinDeliveryDto {
    private Long orderId;
    private Long customerId;
    private String customerName;
    private String phoneNumber;
    private String email;
    private boolean smsSent;
    private boolean emailSent;
}

