package com.rentaltech.techrental.webapi.technician.model;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.staff.model.Task;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qc_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QCReport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "qc_report_id", nullable = false)
    private Long qcReportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 50)
    private QCPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 50)
    private QCResult result;

    @Column(name = "findings", columnDefinition = "text")
    private String findings;

    @Column(name = "accessory_snapshot_url", length = 500)
    private String accessorySnapShotUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @ManyToOne
    @JoinColumn(name = "task_id", referencedColumnName = "task_id", nullable = false)
    private Task task;

    @OneToMany(mappedBy = "qcReport")
    @Builder.Default
    private List<Allocation> allocations = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
