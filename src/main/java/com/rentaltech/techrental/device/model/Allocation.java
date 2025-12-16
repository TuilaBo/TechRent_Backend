package com.rentaltech.techrental.device.model;

import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.webapi.technician.model.QCReport;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Allocation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"qcReport", "baselineSnapshots", "finalSnapshots"})
@EqualsAndHashCode(exclude = {"qcReport", "baselineSnapshots", "finalSnapshots"})
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "allocation_id", nullable = false)
    private Long allocationId;

    @ManyToOne
    @JoinColumn(name = "device_id", referencedColumnName = "device_id")
    private Device device;

    @ManyToOne
    @JoinColumn(name = "order_detail_id", referencedColumnName = "order_detail_id", nullable = false)
    private OrderDetail orderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_report_id")
    private QCReport qcReport;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "allocated_at")
    private LocalDateTime allocatedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @OneToMany(mappedBy = "allocation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Where(clause = "snapshot_type = 'BASELINE'")
    @Builder.Default
    private List<AllocationConditionSnapshot> baselineSnapshots = new ArrayList<>();

    @OneToMany(mappedBy = "allocation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Where(clause = "snapshot_type = 'FINAL'")
    @Builder.Default
    private List<AllocationConditionSnapshot> finalSnapshots = new ArrayList<>();

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
