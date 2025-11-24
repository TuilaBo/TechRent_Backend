package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.AllocationConditionSnapshot;
import com.rentaltech.techrental.device.model.AllocationSnapshotSource;
import com.rentaltech.techrental.device.model.AllocationSnapshotType;
import com.rentaltech.techrental.device.repository.AllocationConditionSnapshotRepository;
import com.rentaltech.techrental.staff.model.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AllocationSnapshotService {

    private final AllocationConditionSnapshotRepository allocationConditionSnapshotRepository;

    @Transactional
    public void createSnapshots(Collection<Allocation> allocations,
                                AllocationSnapshotType snapshotType,
                                AllocationSnapshotSource source,
                                Staff staff) {
        if (CollectionUtils.isEmpty(allocations) || snapshotType == null || source == null) {
            return;
        }
        List<AllocationConditionSnapshot> snapshots = allocations.stream()
                .filter(Objects::nonNull)
                .map(allocation -> AllocationConditionSnapshot.builder()
                        .allocation(allocation)
                        .snapshotType(snapshotType)
                        .source(source)
                        .staff(staff)
                        .build())
                .collect(Collectors.toList());
        if (snapshots.isEmpty()) {
            return;
        }
        allocationConditionSnapshotRepository.saveAll(snapshots);
    }
}
