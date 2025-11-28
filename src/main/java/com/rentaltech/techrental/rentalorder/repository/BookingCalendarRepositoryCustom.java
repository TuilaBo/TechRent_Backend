package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.BookingCalendar;
import com.rentaltech.techrental.rentalorder.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface BookingCalendarRepositoryCustom {

    long countOverlappingByModel(Long deviceModelId,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime,
                                 Collection<BookingStatus> statuses);

    Set<Long> findBusyDeviceIdsByModelAndRange(Long deviceModelId,
                                               LocalDateTime startTime,
                                               LocalDateTime endTime,
                                               Collection<BookingStatus> statuses);

    List<BookingCalendar> findUpcomingBookings(LocalDateTime start,
                                               LocalDateTime end,
                                               Collection<BookingStatus> statuses);
}


