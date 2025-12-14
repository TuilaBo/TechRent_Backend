package com.rentaltech.techrental.rentalorder.model;

import jakarta.persistence.*;
import lombok.*;
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "extension_id", nullable = false, updatable = false)
    private Long extensionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RentalOrder rentalOrder;

    @Column(name = "extension_start", nullable = false)
    private LocalDateTime extensionStart;

    @Column(name = "extension_end", nullable = false)
    private LocalDateTime extensionEnd;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "price_per_day", nullable = false)
    private BigDecimal pricePerDay;

    @Column(name = "additional_price", nullable = false)
    private BigDecimal additionalPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
