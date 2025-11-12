package com.rentaltech.techrental.rentalorder.model;

import com.rentaltech.techrental.device.model.Device;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "BookingCalendar")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @ManyToOne
    @JoinColumn(name = "device_id", referencedColumnName = "device_id", nullable = false)
    private Device device;

    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    private RentalOrder rentalOrder;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

