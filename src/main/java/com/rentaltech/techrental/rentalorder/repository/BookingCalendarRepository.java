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
import java.util.Set;

@Repository
public interface BookingCalendarRepository extends JpaRepository<BookingCalendar, Long> {

    @Query("select count(b) from BookingCalendar b " +
            "where b.device.deviceModel.deviceModelId = :deviceModelId " +
            "and b.status in :statuses " +
            "and b.startTime < :endTime and b.endTime > :startTime")
    long countOverlappingByModel(@Param("deviceModelId") Long deviceModelId,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime,
                                 @Param("statuses") Collection<BookingStatus> statuses);

    @Query("select distinct b.device.deviceId from BookingCalendar b " +
            "where b.device.deviceModel.deviceModelId = :deviceModelId " +
            "and b.status in :statuses " +
            "and b.startTime < :endTime and b.endTime > :startTime")
    Set<Long> findBusyDeviceIdsByModelAndRange(@Param("deviceModelId") Long deviceModelId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("statuses") Collection<BookingStatus> statuses);

    @Transactional
    @Modifying
    void deleteByRentalOrder_OrderId(Long orderId);
}

