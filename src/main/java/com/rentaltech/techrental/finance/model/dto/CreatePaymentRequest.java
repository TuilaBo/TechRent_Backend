package com.rentaltech.techrental.finance.model.dto;

import com.rentaltech.techrental.finance.model.InvoiceType;
import com.rentaltech.techrental.finance.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {

    @NotNull
    private Long orderId;

    @NotNull
    private InvoiceType invoiceType;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String description;

    private String returnUrl;

    private String cancelUrl;

    private String frontendSuccessUrl;

    private String frontendFailureUrl;
}
