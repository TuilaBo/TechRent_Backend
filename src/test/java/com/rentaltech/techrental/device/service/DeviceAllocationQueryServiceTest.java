package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceAllocationQueryServiceTest {

    @Mock
    private AllocationRepository allocationRepository;

    @InjectMocks
    private DeviceAllocationQueryService service;

    @Test
    void returnsEmptyListWhenOrderIdNull() {
        assertThat(service.getAllocatedDevicesForOrder(null)).isEmpty();
        verify(allocationRepository, never()).findByOrderDetail_RentalOrder_OrderId(anyLong());
    }

    @Test
    void mapsAllocationsToDevices() {
        Device device = Device.builder().deviceId(1L).build();
        Allocation allocation = Allocation.builder().device(device).build();
        when(allocationRepository.findByOrderDetail_RentalOrder_OrderId(5L))
                .thenReturn(List.of(allocation));

        List<Device> devices = service.getAllocatedDevicesForOrder(5L);

        assertThat(devices).containsExactly(device);
    }
}
