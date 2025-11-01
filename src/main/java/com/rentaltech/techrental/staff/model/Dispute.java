package com.rentaltech.techrental.staff.model;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Dispute")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "settlement_id", referencedColumnName = "settlement_id", nullable = false)
    private Settlement settlement;

    @Enumerated(EnumType.STRING)
    @Column(name = "open_by", length = 50)
    private DisputeOpenBy openBy;

    @ManyToOne
    @JoinColumn(name = "opened_by_customer_id", referencedColumnName = "customer_id")
    private Customer openedByCustomer;

    @ManyToOne
    @JoinColumn(name = "opened_by_staff_id", referencedColumnName = "staff_id")
    private Staff openedByStaff;

    @Column(name = "reason")
    private String reason;

    @Column(name = "detail")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private DisputeStatus status;

    @Column(name = "open_at")
    private LocalDateTime openAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}

