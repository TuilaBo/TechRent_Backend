package com.rentaltech.techrental.staff.model;

import com.rentaltech.techrental.device.model.Allocation;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "device_replacement_report_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"replacementReport", "allocation"})
@ToString(exclude = {"replacementReport", "allocation"})
public class DeviceReplacementReportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "replacement_report_item_id")
    private Long replacementReportItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "replacement_report_id", nullable = false)
    private DeviceReplacementReport replacementReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    @Column(name = "is_old_device", nullable = false)
    @Builder.Default
    private Boolean isOldDevice = false; // true = device cũ, false = device mới

    @ElementCollection
    @CollectionTable(
            name = "device_replacement_report_item_evidences",
            joinColumns = @JoinColumn(name = "replacement_report_item_id")
    )
    @Column(name = "evidence_url", length = 1000)
    @Builder.Default
    private List<String> evidenceUrls = new ArrayList<>();
}

