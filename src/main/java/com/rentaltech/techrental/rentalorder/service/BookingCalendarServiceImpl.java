package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.rentalorder.model.BookingCalendar;
import com.rentaltech.techrental.rentalorder.model.BookingStatus;
import com.rentaltech.techrental.rentalorder.model.ReservationStatus;
import com.rentaltech.techrental.rentalorder.repository.BookingCalendarRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingCalendarServiceImpl implements BookingCalendarService {

    private final BookingCalendarRepository bookingCalendarRepository;
    private final DeviceRepository deviceRepository;
    private final ReservationService reservationService;

    @Override
    @Transactional
    public void createBookingsForAllocations(List<Allocation> allocations) {
        if (allocations == null || allocations.isEmpty()) return;

        List<BookingCalendar> items = new ArrayList<>();
        allocations.forEach(allocation -> {
            if (allocation == null || allocation.getDevice() == null || allocation.getOrderDetail() == null) return;
            var order = allocation.getOrderDetail().getRentalOrder();
            if (order == null || order.getStartDate() == null || order.getEndDate() == null) return;
            items.add(BookingCalendar.builder()
                    .device(allocation.getDevice())
                    .rentalOrder(order)
                    .startTime(order.getStartDate())
                    .endTime(order.getEndDate())
                    .status(BookingStatus.BOOKED)
                    .build());
        });
        if (!items.isEmpty()) {
            bookingCalendarRepository.saveAll(items);
        }
    }

    @Override
    @Transactional
    public void clearBookingsForOrder(Long orderId) {
        if (orderId == null) return;
        bookingCalendarRepository.deleteByRentalOrder_OrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getAvailableCountByModel(Long deviceModelId, LocalDateTime start, LocalDateTime end) {
        if (deviceModelId == null || start == null || end == null) return 0L;
        if (!start.isBefore(end)) return 0L;
        long total = deviceRepository.countByDeviceModel_DeviceModelId(deviceModelId);
        long booked = bookingCalendarRepository.countOverlappingByModel(
                deviceModelId, start, end, EnumSet.of(BookingStatus.BOOKED, BookingStatus.ACTIVE)
        );
        long reserved = reservationService.countActiveReservedQuantity(deviceModelId, start, end);

        long available = total - booked - reserved;

        Role role = resolveCurrentUserRole();
        if (role == Role.TECHNICIAN) {
            long underReview = reservationService.countReservedQuantityByStatus(
                    deviceModelId, start, end, EnumSet.of(ReservationStatus.UNDER_REVIEW));
            available = total - booked - underReview;
        }

        return Math.max(available, 0);
    }

    private Role resolveCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(granted -> granted.getAuthority())
                .filter(Objects::nonNull)
                .map(value -> value.replace("ROLE_", ""))
                .map(String::toUpperCase)
                .map(name -> {
                    try {
                        return Role.valueOf(name);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}

