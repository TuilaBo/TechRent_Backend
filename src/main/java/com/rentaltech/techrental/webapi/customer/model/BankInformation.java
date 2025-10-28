package com.rentaltech.techrental.webapi.customer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "BankInformation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "bank_information_id", nullable = false)
    private Long bankInformationId;

    @Column(name = "bank_name", length = 100, nullable = false)
    private String bankName;

    @Column(name = "card_number", length = 20, nullable = false)
    private String cardNumber;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", nullable = false)
    private Customer customer;
}
