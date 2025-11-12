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
@Table(name = "handover_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "handover_report_id")
    private Long handoverReportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder rentalOrder;

    @Column(name = "customer_info", nullable = false, length = 500)
    private String customerInfo;

    @Column(name = "technician_info", nullable = false, length = 500)
    private String technicianInfo;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "handover_report_items",
            joinColumns = @JoinColumn(name = "handover_report_id")
    )
    private List<HandoverReportItem> items = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "handover_report_evidences",
            joinColumns = @JoinColumn(name = "handover_report_id")
    )
    @Column(name = "evidence_url", length = 1000)
    private List<String> evidenceUrls = new ArrayList<>();

    @Column(name = "handover_datetime", nullable = false)
    private LocalDateTime handoverDateTime;

    @Column(name = "handover_location", nullable = false, length = 500)
    private String handoverLocation;

    @Column(name = "customer_signature", length = 1000)
    private String customerSignature;
}

