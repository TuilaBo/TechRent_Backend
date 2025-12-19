package com.rentaltech.techrental.staff.model;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "handover_report_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"handoverReport", "allocation"})
@ToString(exclude = {"handoverReport", "allocation"})
public class HandoverReportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "handover_report_item_id")
    private Long handoverReportItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handover_report_id", nullable = false)
    private HandoverReport handoverReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    @ElementCollection
    @CollectionTable(
            name = "handover_report_item_evidences",
            joinColumns = @JoinColumn(name = "handover_report_item_id")
    )
    @Column(name = "evidence_url", length = 1000)
    @Builder.Default
    private List<String> evidenceUrls = new ArrayList<>();

    /**
     * Convenience factory that maps {@link OrderDetail} into a handover item.
     * By default, delivered quantity equals ordered quantity; callers can override later if needed.
     */
    public static HandoverReportItem fromOrderDetail(OrderDetail detail) {
        if (detail == null) {
            return null;
        }
        return HandoverReportItem.builder().build();
    }
}
