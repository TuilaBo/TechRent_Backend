package com.rentaltech.techrental.contract.model;

import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtension;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_extension_annex")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractExtensionAnnex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annex_id")
    private Long annexId;

    @Column(name = "annex_number", nullable = false, unique = true, length = 100)
    private String annexNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_order_extension_id", nullable = false)
    private RentalOrderExtension rentalOrderExtension;

    @Column(name = "original_order_id", nullable = false)
    private Long originalOrderId;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "legal_reference", columnDefinition = "text")
    private String legalReference;

    @Column(name = "extension_reason", columnDefinition = "text")
    private String extensionReason;

    @Column(name = "previous_end_date")
    private LocalDateTime previousEndDate;

    @Column(name = "extension_start_date", nullable = false)
    private LocalDateTime extensionStartDate;

    @Column(name = "extension_end_date", nullable = false)
    private LocalDateTime extensionEndDate;

    @Column(name = "extension_days")
    private Integer extensionDays;

    @Column(name = "extension_fee", precision = 15, scale = 2)
    private BigDecimal extensionFee;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "vat_amount", precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "total_payable", precision = 15, scale = 2)
    private BigDecimal totalPayable;

    @Column(name = "deposit_adjustment", precision = 15, scale = 2)
    private BigDecimal depositAdjustment;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "annex_content", columnDefinition = "text")
    private String annexContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(name = "admin_signed_at")
    private LocalDateTime adminSignedAt;

    @Column(name = "admin_signed_by")
    private Long adminSignedBy;

    @Column(name = "admin_signature_hash", columnDefinition = "text")
    private String adminSignatureHash;

    @Column(name = "customer_signed_at")
    private LocalDateTime customerSignedAt;

    @Column(name = "customer_signed_by")
    private Long customerSignedBy;

    @Column(name = "customer_signature_hash", columnDefinition = "text")
    private String customerSignatureHash;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.issuedAt == null) {
            this.issuedAt = now;
        }
        if (this.effectiveDate == null) {
            this.effectiveDate = this.extensionStartDate;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
