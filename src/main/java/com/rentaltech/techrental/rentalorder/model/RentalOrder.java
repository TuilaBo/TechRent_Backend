package com.rentaltech.techrental.rentalorder.model;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_order")
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

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount;

    @Column(name = "deposit_amount_held", nullable = false)
    private BigDecimal depositAmountHeld;

    @Column(name = "deposit_amount_used")
    private BigDecimal depositAmountUsed;

    @Column(name = "deposit_amount_refunded")
    private BigDecimal depositAmountRefunded;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "price_per_day", nullable = false)
    private BigDecimal pricePerDay;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", nullable = false)
    private Customer customer;
}
