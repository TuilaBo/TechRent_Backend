package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.BookingCalendarRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCalendarServiceImplTest {

    @Mock
    private BookingCalendarRepository bookingCalendarRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private BookingCalendarServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBookingsForAllocationsPersistsValidItems() {
        RentalOrder order = RentalOrder.builder()
                .orderId(1L)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .build();
        Allocation allocation = Allocation.builder()
                .device(Device.builder().deviceId(10L).build())
                .orderDetail(OrderDetail.builder().rentalOrder(order).build())
                .build();

        service.createBookingsForAllocations(List.of(allocation));

        verify(bookingCalendarRepository).saveAll(anyList());
    }

    @Test
    void createBookingsForAllocationsNoopsOnNullOrEmptyInput() {
        service.createBookingsForAllocations(null);
        service.createBookingsForAllocations(List.of());
        verify(bookingCalendarRepository, org.mockito.Mockito.never()).saveAll(anyList());
    }

    @Test
    void createBookingsForAllocationsSkipsWhenAllocationMissingDevice() {
        RentalOrder order = RentalOrder.builder()
                .orderId(1L)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        Allocation allocation = Allocation.builder()
                .device(null) // missing device
                .orderDetail(OrderDetail.builder().rentalOrder(order).build())
                .build();

        service.createBookingsForAllocations(List.of(allocation));

        verify(bookingCalendarRepository, org.mockito.Mockito.never()).saveAll(anyList());
    }

    @Test
    void createBookingsForAllocationsSkipsWhenOrderDatesMissing() {
        RentalOrder orderMissingDates = RentalOrder.builder()
                .orderId(1L)
                .startDate(null)
                .endDate(null)
                .build();
        Allocation allocation = Allocation.builder()
                .device(Device.builder().deviceId(10L).build())
                .orderDetail(OrderDetail.builder().rentalOrder(orderMissingDates).build())
                .build();

        service.createBookingsForAllocations(List.of(allocation));

        verify(bookingCalendarRepository, org.mockito.Mockito.never()).saveAll(anyList());
    }

    @Test
    void getAvailableCountByModelExcludesBusyDamagedAndReserved() {
        Device available = Device.builder()
                .deviceId(1L)
                .status(DeviceStatus.AVAILABLE)
                .deviceModel(DeviceModel.builder().deviceModelId(5L).build())
                .build();
        Device damaged = Device.builder()
                .deviceId(2L)
                .status(DeviceStatus.DAMAGED)
                .deviceModel(DeviceModel.builder().deviceModelId(5L).build())
                .build();
        when(deviceRepository.findByDeviceModel_DeviceModelId(5L)).thenReturn(List.of(available, damaged));
        when(bookingCalendarRepository.findBusyDeviceIdsByModelAndRange(eq(5L), any(), any(), any()))
                .thenReturn(Set.of());
        when(reservationService.countActiveReservedQuantity(eq(5L), any(), any())).thenReturn(0L);

        long count = service.getAvailableCountByModel(5L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1));

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void technicianRoleUsesUnderReviewReservations() {
        Device available = Device.builder()
                .deviceId(3L)
                .status(DeviceStatus.AVAILABLE)
                .build();
        when(deviceRepository.findByDeviceModel_DeviceModelId(7L)).thenReturn(List.of(available));
        when(bookingCalendarRepository.findBusyDeviceIdsByModelAndRange(eq(7L), any(), any(), any()))
                .thenReturn(Set.of());
        when(reservationService.countReservedQuantityByStatus(eq(7L), any(), any(), any())).thenReturn(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("tech", "pass", "ROLE_TECHNICIAN")
        );

        long count = service.getAvailableCountByModel(7L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1));

        assertThat(count).isZero();
    }

    @Test
    void getAvailableCountByModelReturnsZeroWhenInvalidRange() {
        long count = service.getAvailableCountByModel(5L,
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(1));

        assertThat(count).isZero();
    }

    @Test
    void getAvailableCountByModelReturnsZeroWhenModelIdNull() {
        long count = service.getAvailableCountByModel(null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1));

        assertThat(count).isZero();
    }
}
