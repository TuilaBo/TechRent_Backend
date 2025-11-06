package com.rentaltech.techrental.finance.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePaymentResponse {
    private String paymentLinkId;
    private String checkoutUrl;
    private String qrCodeUrl;
    private Long orderCode;
    private String status;
}
