package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.Reservation;
import com.rentaltech.techrental.rentalorder.model.ReservationStatus;
import com.rentaltech.techrental.rentalorder.repository.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
        SecurityContextHolder.clearContext();
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

    @AfterEach
    void tearDownContext() {
        SecurityContextHolder.clearContext();
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
    void createPendingReservationsNoopsWhenOrderNullOrDetailsEmpty() {
        service.createPendingReservations(null, List.of(detail));
        service.createPendingReservations(order, List.of());
        verify(reservationRepository, org.mockito.Mockito.never()).saveAll(anyList());
    }

    @Test
    void createPendingReservationsSkipsInvalidDetails() {
        OrderDetail invalid = OrderDetail.builder()
                .orderDetailId(4L)
                .deviceModel(null)
                .quantity(-1L)
                .rentalOrder(order)
                .build();

        service.createPendingReservations(order, List.of(invalid));

        verify(reservationRepository, org.mockito.Mockito.never()).saveAll(anyList());
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
    void cancelReservationsNoopsWhenOrderIdNull() {
        service.cancelReservations(null);

        verify(reservationRepository, never()).overrideStatusForOrder(anyLong(), any(), any());
    }

    @Test
    void cancelReservationsCanBeInvokedMultipleTimes() {
        service.cancelReservations(9L);
        service.cancelReservations(10L);

        verify(reservationRepository).overrideStatusForOrder(eq(9L), eq(ReservationStatus.CANCELLED), isNull());
        verify(reservationRepository).overrideStatusForOrder(eq(10L), eq(ReservationStatus.CANCELLED), isNull());
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
    void countActiveReservedQuantityReturnsZeroWhenInvalidInput() {
        long resultNullId = service.countActiveReservedQuantity(null, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        long resultInvalidRange = service.countActiveReservedQuantity(1L, LocalDateTime.now(), LocalDateTime.now().minusDays(1));

        assertThat(resultNullId).isZero();
        assertThat(resultInvalidRange).isZero();
        verify(reservationRepository, never()).sumReservedQuantity(anyLong(), any(), any(), anySet(), any());
    }

    @Test
    void countActiveReservedQuantityUsesTechnicianStatuses() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("tech", "pass", "ROLE_TECHNICIAN")
        );
        when(reservationRepository.sumReservedQuantity(anyLong(), any(), any(), anySet(), any())).thenReturn(3L);

        long result = service.countActiveReservedQuantity(2L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1));

        assertThat(result).isEqualTo(3L);
        ArgumentCaptor<Set<ReservationStatus>> captor = ArgumentCaptor.forClass(Set.class);
        verify(reservationRepository).sumReservedQuantity(eq(2L), any(), any(), captor.capture(), any());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(ReservationStatus.UNDER_REVIEW);
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
