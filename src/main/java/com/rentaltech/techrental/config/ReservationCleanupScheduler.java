package com.rentaltech.techrental.config;

import com.rentaltech.techrental.rentalorder.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final ReservationService reservationService;

    /**
     * Periodically mark overdue reservations as expired so availability stays fresh.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void expireStaleReservations() {
        try {
            reservationService.expireReservations();
        } catch (Exception ex) {
            log.warn("Failed to expire stale reservations: {}", ex.getMessage(), ex);
        }
    }
}
