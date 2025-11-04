package com.rentaltech.techrental.rentalorder.model;

import com.rentaltech.techrental.device.model.DeviceModel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "OrderDetail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "order_detail_id", nullable = false)
    private Long orderDetailId;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "price_per_day", nullable = false)
    private BigDecimal pricePerDay;

    @Column(name = "deposit_amount_per_unit", nullable = false)
    private BigDecimal depositAmountPerUnit;

    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    private RentalOrder rentalOrder;

    @ManyToOne
    @JoinColumn(name = "device_model_id", referencedColumnName = "device_model_id", nullable = false)
    private DeviceModel deviceModel;
}
