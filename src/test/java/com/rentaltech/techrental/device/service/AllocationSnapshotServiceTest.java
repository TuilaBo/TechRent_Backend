package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.AllocationConditionSnapshot;
import com.rentaltech.techrental.device.model.AllocationSnapshotSource;
import com.rentaltech.techrental.device.model.AllocationSnapshotType;
import com.rentaltech.techrental.device.repository.AllocationConditionSnapshotRepository;
import com.rentaltech.techrental.staff.model.Staff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AllocationSnapshotServiceTest {

    @Mock
    private AllocationConditionSnapshotRepository repository;

    @InjectMocks
    private AllocationSnapshotService service;

    @Test
    void createSnapshotsPersistsEntries() {
        Allocation allocation = Allocation.builder().allocationId(1L).build();
        Staff staff = Staff.builder().staffId(2L).build();

        service.createSnapshots(List.of(allocation), AllocationSnapshotType.BASELINE, AllocationSnapshotSource.QC_BEFORE, staff);

        ArgumentCaptor<List<AllocationConditionSnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getAllocation()).isEqualTo(allocation);
    }

    @Test
    void createSnapshotsSkipsWhenInvalidInput() {
        service.createSnapshots(List.of(), null, AllocationSnapshotSource.QC_BEFORE, null);
        verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
