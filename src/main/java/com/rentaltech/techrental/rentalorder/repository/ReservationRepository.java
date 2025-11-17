package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.Reservation;
import com.rentaltech.techrental.rentalorder.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select coalesce(sum(r.reservedQuantity), 0)
            from Reservation r
            where r.deviceModel.deviceModelId = :deviceModelId
              and r.startTime < :endTime
              and r.endTime > :startTime
              and r.status in :statuses
              and (r.expirationTime is null or r.expirationTime > :referenceTime)
            """)
    long sumReservedQuantity(@Param("deviceModelId") Long deviceModelId,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime,
                             @Param("statuses") Collection<ReservationStatus> statuses,
                             @Param("referenceTime") LocalDateTime referenceTime);

    @Query("""
            select coalesce(sum(r.reservedQuantity), 0)
            from Reservation r
            where r.deviceModel.deviceModelId = :deviceModelId
              and r.startTime < :endTime
              and r.endTime > :startTime
              and r.status in :statuses
            """)
    long sumReservedQuantityByStatus(@Param("deviceModelId") Long deviceModelId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("statuses") Collection<ReservationStatus> statuses);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Reservation r
               set r.status = :expiredStatus
             where r.status in :statuses
               and r.expirationTime is not null
               and r.expirationTime <= :referenceTime
            """)
    int markExpired(@Param("statuses") Collection<ReservationStatus> statuses,
                    @Param("referenceTime") LocalDateTime referenceTime,
                    @Param("expiredStatus") ReservationStatus expiredStatus);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Reservation r
               set r.status = :newStatus,
                   r.expirationTime = :expirationTime
             where r.orderDetail.rentalOrder.orderId = :orderId
               and r.status in :currentStatuses
            """)
    int updateStatusAndExpiration(@Param("orderId") Long orderId,
                                  @Param("currentStatuses") Collection<ReservationStatus> currentStatuses,
                                  @Param("newStatus") ReservationStatus newStatus,
                                  @Param("expirationTime") LocalDateTime expirationTime);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Reservation r
               set r.status = :newStatus,
                   r.expirationTime = :expirationTime
             where r.orderDetail.rentalOrder.orderId = :orderId
            """)
    int overrideStatusForOrder(@Param("orderId") Long orderId,
                               @Param("newStatus") ReservationStatus newStatus,
                               @Param("expirationTime") LocalDateTime expirationTime);

    void deleteByOrderDetail_RentalOrder_OrderId(Long orderId);
}
