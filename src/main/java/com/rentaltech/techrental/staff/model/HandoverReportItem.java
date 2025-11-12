package com.rentaltech.techrental.staff.model;

import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportItem {

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "ordered_quantity")
    private Long orderedQuantity;

    @Column(name = "delivered_quantity")
    private Long deliveredQuantity;

    /**
     * Convenience factory that maps {@link OrderDetail} into a handover item.
     * By default, delivered quantity equals ordered quantity; callers can override later if needed.
     */
    public static HandoverReportItem fromOrderDetail(OrderDetail detail) {
        if (detail == null) {
            return null;
        }

        DeviceModel deviceModel = detail.getDeviceModel();
        String itemName = deviceModel != null ? deviceModel.getDeviceName() : null;
        String itemCode = deviceModel != null && deviceModel.getDeviceModelId() != null
                ? deviceModel.getDeviceModelId().toString()
                : null;

        return HandoverReportItem.builder()
                .itemName(itemName)
                .itemCode(itemCode)
                .unit("unit")
                .orderedQuantity(detail.getQuantity())
                .deliveredQuantity(detail.getQuantity())
                .build();
    }
}