package com.rentaltech.techrental.webapi.customer.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "RentalOrder")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "order_id", updatable = false, nullable = false)
    private Long orderId;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "deposit_amount", nullable = false)
    private Double depositAmount;

    @Column(name = "deposit_amount_held", nullable = false)
    private Double depositAmountHeld;

    @Column(name = "deposit_amount_used")
    private Double depositAmountUsed;

    @Column(name = "deposit_amount_refunded")
    private Double depositAmountRefunded;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "price_per_day", nullable = false)
    private Double pricePerDay;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", nullable = false)
    private Customer customer;
}
