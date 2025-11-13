package com.rentaltech.techrental.finance.model.dto;

import com.rentaltech.techrental.finance.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto {
    private Long transactionId;
    private BigDecimal amount;
    private TrasactionType transactionType;
    private LocalDateTime createdAt;
    private String createdBy;
    private Long invoiceId;
    private Long rentalOrderId;
    private PaymentMethod paymentMethod;
    private InvoiceStatus invoiceStatus;

    public static TransactionResponseDto from(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        Invoice invoice = transaction.getInvoice();
        Long invoiceId = invoice != null ? invoice.getInvoiceId() : null;
        Long rentalOrderId = (invoice != null && invoice.getRentalOrder() != null)
                ? invoice.getRentalOrder().getOrderId()
                : null;
        PaymentMethod paymentMethod = invoice != null ? invoice.getPaymentMethod() : null;
        InvoiceStatus status = invoice != null ? invoice.getInvoiceStatus() : null;
        return TransactionResponseDto.builder()
                .transactionId(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .createdAt(transaction.getCreatedAt())
                .createdBy(transaction.getCreatedBy())
                .invoiceId(invoiceId)
                .rentalOrderId(rentalOrderId)
                .paymentMethod(paymentMethod)
                .invoiceStatus(status)
                .build();
    }
}
