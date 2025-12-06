package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.Reservation;
import com.rentaltech.techrental.rentalorder.model.ReservationStatus;
import com.rentaltech.techrental.rentalorder.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationServiceImpl service;

    private RentalOrder order;
    private OrderDetail detail;

    @BeforeEach
    void setUp() {
        order = RentalOrder.builder()
                .orderId(1L)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .build();
        detail = OrderDetail.builder()
                .orderDetailId(3L)
                .deviceModel(DeviceModel.builder().deviceModelId(5L).build())
                .quantity(2L)
                .rentalOrder(order)
                .build();
    }

    @Test
    void createPendingReservationsPersistsValidDetails() {
        service.createPendingReservations(order, List.of(detail));

        ArgumentCaptor<List<Reservation>> captor = ArgumentCaptor.forClass(List.class);
        verify(reservationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getStatus()).isEqualTo(ReservationStatus.PENDING_REVIEW);
    }

    @Test
    void moveToUnderReviewUsesUpdateStatus() {
        service.moveToUnderReview(10L);

        verify(reservationRepository).updateStatusAndExpiration(eq(10L),
                eq(EnumSet.of(ReservationStatus.PENDING_REVIEW, ReservationStatus.UNDER_REVIEW, ReservationStatus.EXPIRED)),
                eq(ReservationStatus.UNDER_REVIEW),
                any(LocalDateTime.class));
    }

    @Test
    void markConfirmedSetsStatusWithoutExpiration() {
        service.markConfirmed(4L);

        verify(reservationRepository).updateStatusAndExpiration(eq(4L),
                eq(EnumSet.of(ReservationStatus.PENDING_REVIEW, ReservationStatus.UNDER_REVIEW, ReservationStatus.CONFIRMED)),
                eq(ReservationStatus.CONFIRMED),
                isNull());
    }

    @Test
    void cancelReservationsOverridesStatus() {
        service.cancelReservations(8L);

        verify(reservationRepository).overrideStatusForOrder(eq(8L), eq(ReservationStatus.CANCELLED), isNull());
    }

    @Test
    void expireReservationsDelegatesToRepository() {
        service.expireReservations();

        verify(reservationRepository).markExpired(eq(EnumSet.of(ReservationStatus.PENDING_REVIEW, ReservationStatus.UNDER_REVIEW)),
                any(LocalDateTime.class),
                eq(ReservationStatus.EXPIRED));
    }

    @Test
    void countActiveReservedQuantityReturnsRepositorySum() {
        when(reservationRepository.sumReservedQuantity(anyLong(), any(), any(), anySet(), any())).thenReturn(5L);

        long result = service.countActiveReservedQuantity(1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1));

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void countReservedQuantityByStatusValidatesInput() {
        long result = service.countReservedQuantityByStatus(null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                EnumSet.of(ReservationStatus.PENDING_REVIEW));
        assertThat(result).isZero();
    }
}
