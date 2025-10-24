package com.rentaltech.techrental.device.model;

import com.rentaltech.techrental.webapi.customer.model.OrderDetail;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Allocation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "allocation_id", nullable = false)
    private Long allocationId;

    @ManyToOne
    @JoinColumn(name = "device_id", referencedColumnName = "device_id")
    private Device device;

    @ManyToOne
    @JoinColumn(name = "order_detail_id", referencedColumnName = "order_detail_id", nullable = false)
    private OrderDetail orderDetail;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "allocated_at")
    private LocalDateTime allocatedAt;
}
