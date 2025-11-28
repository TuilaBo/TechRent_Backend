package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.BookingCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BookingCalendarRepository extends JpaRepository<BookingCalendar, Long>, BookingCalendarRepositoryCustom {

    @Transactional
    @Modifying
    void deleteByRentalOrder_OrderId(Long orderId);
}

