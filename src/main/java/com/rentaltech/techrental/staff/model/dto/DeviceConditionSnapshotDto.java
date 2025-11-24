package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.device.model.AllocationConditionDetail;
import com.rentaltech.techrental.device.model.AllocationConditionSnapshot;
import com.rentaltech.techrental.device.model.AllocationSnapshotSource;
import com.rentaltech.techrental.device.model.AllocationSnapshotType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DeviceConditionSnapshotDto {
    private Long snapshotId;
    private AllocationSnapshotType snapshotType;
    private AllocationSnapshotSource source;
    private List<AllocationConditionDetail> conditionDetails;
    private List<String> images;
    private LocalDateTime createdAt;

    public static DeviceConditionSnapshotDto fromEntity(AllocationConditionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return DeviceConditionSnapshotDto.builder()
                .snapshotId(snapshot.getAllocationConditionSnapshotId())
                .snapshotType(snapshot.getSnapshotType())
                .source(snapshot.getSource())
                .conditionDetails(snapshot.getConditionDetails() == null ? List.of() : List.copyOf(snapshot.getConditionDetails()))
                .images(snapshot.getImages() == null ? List.of() : List.copyOf(snapshot.getImages()))
                .createdAt(snapshot.getCreatedAt())
                .build();
    }
}
