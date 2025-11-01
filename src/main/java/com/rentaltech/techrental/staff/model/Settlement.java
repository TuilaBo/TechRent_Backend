package com.rentaltech.techrental.staff.model;

import com.rentaltech.techrental.webapi.customer.model.RentalOrder;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Settlement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    private RentalOrder rentalOrder;

    @Column(name = "total_rent", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalRent;

    @Column(name = "damage_fee", precision = 19, scale = 2)
    private BigDecimal damageFee;

    @Column(name = "late_fee", precision = 19, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "accessory_fee", precision = 19, scale = 2)
    private BigDecimal accessoryFee;

    @Column(name = "deposit_used", precision = 19, scale = 2)
    private BigDecimal depositUsed;

    @Column(name = "final_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 50)
    private SettlementState state;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
}

