package com.rentaltech.techrental.device.model;

import com.rentaltech.techrental.common.converter.StringListJsonConverter;
import com.rentaltech.techrental.staff.model.Staff;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "device_condition")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_condition_id")
    private Long deviceConditionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condition_definition_id", nullable = false)
    private ConditionDefinition conditionDefinition;

    @Column(name = "severity", length = 100)
    private String severity;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "images", columnDefinition = "text")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captured_by_staff_id")
    private Staff capturedBy;

    @PrePersist
    public void onCreate() {
        if (capturedAt == null) {
            capturedAt = LocalDateTime.now();
        }
    }
}
