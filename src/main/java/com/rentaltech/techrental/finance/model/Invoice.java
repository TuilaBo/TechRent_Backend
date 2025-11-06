package com.rentaltech.techrental.finance.model;

import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder rentalOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false)
    private InvoiceType invoiceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "sub_total", nullable = false)
    private BigDecimal subTotal;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "deposit_applied")
    private BigDecimal depositApplied;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status", nullable = false)
    private InvoiceStatus invoiceStatus;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

}
