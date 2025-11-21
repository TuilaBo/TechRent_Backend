package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.Reservation;
import com.rentaltech.techrental.rentalorder.model.ReservationStatus;
import com.rentaltech.techrental.rentalorder.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private static final Duration INITIAL_HOLD_DURATION = Duration.ofMinutes(15);
    private static final Duration UNDER_REVIEW_DURATION = Duration.ofHours(6);
    private static final Set<ReservationStatus> ACTIVE_STATUSES =
            EnumSet.of(ReservationStatus.PENDING_REVIEW, ReservationStatus.UNDER_REVIEW);

    private final ReservationRepository reservationRepository;

    @Override
    public void createPendingReservations(RentalOrder order, List<OrderDetail> details) {
        if (order == null || details == null || details.isEmpty()) {
            return;
        }
        LocalDateTime reservationStart = order.getStartDate();
        LocalDateTime reservationEnd = order.getEndDate();
        if (reservationStart == null || reservationEnd == null || !reservationStart.isBefore(reservationEnd)) {
            return;
        }
        LocalDateTime expirationTime = LocalDateTime.now().plus(INITIAL_HOLD_DURATION);
        List<Reservation> reservations = new ArrayList<>();
        for (OrderDetail detail : details) {
            if (detail == null || detail.getDeviceModel() == null) {
                continue;
            }
            int quantity = detail.getQuantity() != null ? Math.toIntExact(detail.getQuantity()) : 0;
            if (quantity <= 0) {
                continue;
            }
            reservations.add(Reservation.builder()
                    .deviceModel(detail.getDeviceModel())
                    .orderDetail(detail)
                    .startTime(reservationStart)
                    .endTime(reservationEnd)
                    .reservedQuantity(quantity)
                    .status(ReservationStatus.PENDING_REVIEW)
                    .expirationTime(expirationTime)
                    .build());
        }
        if (!reservations.isEmpty()) {
            reservationRepository.saveAll(reservations);
        }
    }

    @Override
    public void moveToUnderReview(Long orderId) {
        updateStatus(orderId,
                EnumSet.of(ReservationStatus.PENDING_REVIEW, ReservationStatus.UNDER_REVIEW, ReservationStatus.EXPIRED),
                ReservationStatus.UNDER_REVIEW,
                LocalDateTime.now().plus(UNDER_REVIEW_DURATION));
    }

    @Override
    public void markConfirmed(Long orderId) {
        updateStatus(orderId,
                EnumSet.of(ReservationStatus.PENDING_REVIEW, ReservationStatus.UNDER_REVIEW, ReservationStatus.CONFIRMED),
                ReservationStatus.CONFIRMED,
                null);
    }

    @Override
    public void cancelReservations(Long orderId) {
        overrideStatus(orderId, ReservationStatus.CANCELLED, null);
    }

    @Override
    public void expireReservations() {
        reservationRepository.markExpired(
                EnumSet.of(ReservationStatus.PENDING_REVIEW, ReservationStatus.UNDER_REVIEW),
                LocalDateTime.now(),
                ReservationStatus.EXPIRED
        );
    }

    @Override
    public long countActiveReservedQuantity(Long deviceModelId, LocalDateTime start, LocalDateTime end) {
        if (deviceModelId == null || start == null || end == null || !start.isBefore(end)) {
            return 0L;
        }
        return reservationRepository.sumReservedQuantity(
                deviceModelId,
                start,
                end,
                ACTIVE_STATUSES,
                LocalDateTime.now()
        );
    }

    @Override
    public long countReservedQuantityByStatus(Long deviceModelId, LocalDateTime start, LocalDateTime end, Collection<ReservationStatus> statuses) {
        if (deviceModelId == null || start == null || end == null || !start.isBefore(end) || statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return reservationRepository.sumReservedQuantityByStatus(deviceModelId, start, end, statuses);
    }

    private void updateStatus(Long orderId,
                              Collection<ReservationStatus> allowedCurrentStatuses,
                              ReservationStatus targetStatus,
                              LocalDateTime expirationTime) {
        if (orderId == null) {
            return;
        }
        if (allowedCurrentStatuses == null || allowedCurrentStatuses.isEmpty()) {
            overrideStatus(orderId, targetStatus, expirationTime);
            return;
        }
        int affected = reservationRepository.updateStatusAndExpiration(
                orderId,
                allowedCurrentStatuses,
                targetStatus,
                expirationTime
        );
        if (affected == 0 && targetStatus == ReservationStatus.CANCELLED) {
            overrideStatus(orderId, targetStatus, expirationTime);
        }
    }

    private void overrideStatus(Long orderId, ReservationStatus targetStatus, LocalDateTime expirationTime) {
        if (orderId == null) {
            return;
        }
        reservationRepository.overrideStatusForOrder(orderId, targetStatus, expirationTime);
    }
}
