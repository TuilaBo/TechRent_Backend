package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DeviceAllocationQueryService {

    private final AllocationRepository allocationRepository;

    @Transactional(readOnly = true)
    public List<Device> getAllocatedDevicesForOrder(Long orderId) {
        if (orderId == null) {
            return List.of();
        }
        return allocationRepository.findByOrderDetail_RentalOrder_OrderId(orderId).stream()
                .map(Allocation::getDevice)
                .filter(Objects::nonNull)
                .toList();
    }
}
