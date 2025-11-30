package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.BookingCalendar;
import com.rentaltech.techrental.rentalorder.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface BookingCalendarRepository extends JpaRepository<BookingCalendar, Long>, BookingCalendarRepositoryCustom {

    @Transactional
    @Modifying
    void deleteByRentalOrder_OrderId(Long orderId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BookingCalendar b " +
            "WHERE b.device.deviceId IN :deviceIds AND b.rentalOrder.orderId <> :excludeOrderId " +
            "AND b.status IN :statuses AND b.startTime < :endTime AND b.endTime > :startTime")
    boolean existsOverlappingForDevices(@Param("deviceIds") Collection<Long> deviceIds,
                                        @Param("excludeOrderId") Long excludeOrderId,
                                        @Param("statuses") Collection<BookingStatus> statuses,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
}

