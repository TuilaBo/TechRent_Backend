package com.rentaltech.techrental.device.model;

import com.rentaltech.techrental.common.converter.AllocationConditionDetailListConverter;
import com.rentaltech.techrental.common.converter.StringListJsonConverter;
import com.rentaltech.techrental.staff.model.Staff;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "allocation_condition_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"allocation", "staff"})
@EqualsAndHashCode(exclude = {"allocation", "staff"})
public class AllocationConditionSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_condition_snapshot_id")
    private Long allocationConditionSnapshotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allocation_id", nullable = false)
    private Allocation allocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false, length = 20)
    private AllocationSnapshotType snapshotType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private AllocationSnapshotSource source;

    @Convert(converter = AllocationConditionDetailListConverter.class)
    @Column(name = "condition_details", columnDefinition = "text")
    @Builder.Default
    private List<AllocationConditionDetail> conditionDetails = new ArrayList<>();

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "images", columnDefinition = "text")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (conditionDetails == null) {
            conditionDetails = new ArrayList<>();
        }
        if (images == null) {
            images = new ArrayList<>();
        }
    }
}
