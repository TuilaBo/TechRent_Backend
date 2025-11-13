package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.device.model.Allocation;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingCalendarService {
    void createBookingsForAllocations(List<Allocation> allocations);
    void clearBookingsForOrder(Long orderId);
    long getAvailableCountByModel(Long deviceModelId, LocalDateTime start, LocalDateTime end);
}

