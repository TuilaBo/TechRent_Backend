package com.rentaltech.techrental.webapi.customer.model;

import com.rentaltech.techrental.authentication.model.Account;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @NotNull(message = "Account không được để trống")
    private Account account;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "full_name", length = 100, nullable = true)
    private String fullName;

    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    @OneToMany(mappedBy = "customer", fetch = FetchType.EAGER)
    private List<ShippingAddress> shippingAddresses;

    @OneToMany(mappedBy = "customer", fetch =  FetchType.EAGER)
    private List<BankInformation> bankInformations;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.INACTIVE;

    // KYC fields
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    @Builder.Default
    private KYCStatus kycStatus = KYCStatus.NOT_STARTED;

    @Column(name = "kyc_front_cccd_url", length = 500)
    private String kycFrontCCCDUrl; // Ảnh mặt trước CCCD

    @Column(name = "kyc_back_cccd_url", length = 500)
    private String kycBackCCCDUrl; // Ảnh mặt sau CCCD

    @Column(name = "kyc_selfie_url", length = 500)
    private String kycSelfieUrl; // Ảnh selfie

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt; // Ngày xác minh

    @Column(name = "kyc_verified_by")
    private Long kycVerifiedBy; // ID người xác minh

    @Column(name = "kyc_rejection_reason", length = 1000)
    private String kycRejectionReason; // Lý do từ chối

    @Column(name = "kyc_rejection_count", nullable = false)
    @Builder.Default
    private Integer kycRejectionCount = 0; // Số lần bị từ chối KYC

    // KYC personal information fields
    @Column(name = "identification_code", length = 20)
    private String identificationCode; // Số CCCD/CMND/Passport

    @Column(name = "type_of_identification", length = 20)
    private String typeOfIdentification; // Loại giấy tờ

    @Column(name = "birthday")
    private LocalDate birthday; // Ngày sinh

    @Column(name = "expiration_date")
    private LocalDate expirationDate; // Ngày hết hạn

    @Column(name = "permanent_address", length = 500)
    private String permanentAddress; // Địa chỉ thường trú

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
