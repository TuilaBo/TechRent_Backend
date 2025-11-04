package com.rentaltech.techrental.finance.model.dto;

import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.model.InvoiceStatus;
import com.rentaltech.techrental.finance.model.InvoiceType;
import com.rentaltech.techrental.finance.model.PaymentMethod;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class InvoiceResponseDto {
    Long invoiceId;
    Long rentalOrderId;
    InvoiceType invoiceType;
    PaymentMethod paymentMethod;
    InvoiceStatus invoiceStatus;
    BigDecimal subTotal;
    BigDecimal taxAmount;
    BigDecimal discountAmount;
    BigDecimal totalAmount;
    BigDecimal depositApplied;
    LocalDateTime paymentDate;
    LocalDateTime dueDate;
    LocalDateTime issueDate;
    String pdfUrl;

    public static InvoiceResponseDto from(Invoice invoice) {
        return InvoiceResponseDto.builder()
                .invoiceId(invoice.getInvoiceId())
                .rentalOrderId(invoice.getRentalOrder() != null ? invoice.getRentalOrder().getOrderId() : null)
                .invoiceType(invoice.getInvoiceType())
                .paymentMethod(invoice.getPaymentMethod())
                .invoiceStatus(invoice.getInvoiceStatus())
                .subTotal(invoice.getSubTotal())
                .taxAmount(invoice.getTaxAmount())
                .discountAmount(invoice.getDiscountAmount())
                .totalAmount(invoice.getTotalAmount())
                .depositApplied(invoice.getDepositApplied())
                .paymentDate(invoice.getPaymentDate())
                .dueDate(invoice.getDueDate())
                .issueDate(invoice.getIssueDate())
                .pdfUrl(invoice.getPdfUrl())
                .build();
    }
}
