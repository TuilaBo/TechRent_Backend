package com.rentaltech.techrental.rentalorder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_order_extension")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrderExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "extension_id")
    private Long extensionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_order_id", nullable = false)
    private RentalOrder originalOrder;

    @Column(name = "extension_start_date", nullable = false)
    private LocalDateTime extensionStartDate;

    @Column(name = "extension_end_date", nullable = false)
    private LocalDateTime extensionEndDate;

    @Column(name = "extension_days", nullable = false)
    private Integer extensionDays;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RentalOrderExtensionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;
}
