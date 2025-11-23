package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.AllocationConditionSnapshot;
import com.rentaltech.techrental.device.model.AllocationSnapshotSource;
import com.rentaltech.techrental.device.model.AllocationSnapshotType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllocationConditionSnapshotRepository extends JpaRepository<AllocationConditionSnapshot, Long> {
    List<AllocationConditionSnapshot> findByAllocation_AllocationId(Long allocationId);

    List<AllocationConditionSnapshot> findByAllocation_AllocationIdAndSnapshotType(Long allocationId, AllocationSnapshotType snapshotType);

    void deleteByAllocation_AllocationIdAndSnapshotTypeAndSource(Long allocationId,
                                                                 AllocationSnapshotType snapshotType,
                                                                 AllocationSnapshotSource source);
}
