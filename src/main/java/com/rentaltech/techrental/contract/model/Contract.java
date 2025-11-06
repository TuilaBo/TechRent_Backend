package com.rentaltech.techrental.contract.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;
    
    @Column(name = "contract_number", nullable = false, unique = true, columnDefinition = "text")
    private String contractNumber;
    
    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;
    
    @Column(name = "description", columnDefinition = "text")
    private String description;
    
    @Column(name = "contract_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ContractType contractType;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ContractStatus status = ContractStatus.DRAFT;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "staff_id")
    private Long staffId; // Staff tạo contract
    
    @Column(name = "order_id")
    private Long orderId; // Link to RentalOrder
    
    @Column(name = "contract_content", columnDefinition = "text")
    private String contractContent; // Nội dung contract HTML/PDF
    
    @Column(name = "terms_and_conditions", columnDefinition = "text")
    private String termsAndConditions;
    
    @Column(name = "rental_period_days")
    private Integer rentalPeriodDays;
    
    @Column(name = "total_amount", precision = 15, scale = 2)
    private java.math.BigDecimal totalAmount;
    
    @Column(name = "deposit_amount", precision = 15, scale = 2)
    private java.math.BigDecimal depositAmount;
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "signed_at")
    private LocalDateTime signedAt;
    
    @Column(name = "admin_signed_at")
    private LocalDateTime adminSignedAt;
    
    @Column(name = "admin_signed_by")
    private Long adminSignedBy;
    
    @Column(name = "customer_signed_at")
    private LocalDateTime customerSignedAt;
    
    @Column(name = "customer_signed_by")
    private Long customerSignedBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
