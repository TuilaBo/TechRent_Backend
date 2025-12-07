package com.rentaltech.techrental.staff.model;

import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "device_replacement_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceReplacementReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "replacement_report_id")
    private Long replacementReportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder rentalOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_staff_id")
    private Staff createdByStaff;

    @Column(name = "customer_info", nullable = false, length = 500)
    private String customerInfo;

    @Column(name = "technician_info", nullable = false, length = 500)
    private String technicianInfo;

    @Builder.Default
    @OneToMany(mappedBy = "replacementReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeviceReplacementReportItem> items = new ArrayList<>();

    @Column(name = "replacement_datetime", nullable = false)
    private LocalDateTime replacementDateTime;

    @Column(name = "replacement_location", nullable = false, length = 500)
    private String replacementLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeviceReplacementReportStatus status = DeviceReplacementReportStatus.PENDING_STAFF_SIGNATURE;

    @Column(name = "staff_signed", nullable = false)
    @Builder.Default
    private Boolean staffSigned = false;

    @Column(name = "staff_signed_at")
    private LocalDateTime staffSignedAt;

    @Column(name = "staff_signature", length = 1000)
    private String staffSignature;

    @Column(name = "customer_signed", nullable = false)
    @Builder.Default
    private Boolean customerSigned = false;

    @Column(name = "customer_signed_at")
    private LocalDateTime customerSignedAt;

    @Column(name = "customer_signature", length = 1000)
    private String customerSignature;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

