package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationService {
    void createPendingReservations(RentalOrder order, List<OrderDetail> details);
    void moveToUnderReview(Long orderId);
    void markConfirmed(Long orderId);
    void cancelReservations(Long orderId);
    void expireReservations();
    long countActiveReservedQuantity(Long deviceModelId, LocalDateTime start, LocalDateTime end);
}
