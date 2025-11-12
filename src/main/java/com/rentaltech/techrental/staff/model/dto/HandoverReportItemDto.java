package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.HandoverReportItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportItemDto {
    private String itemName;
    private String itemCode;
    private String unit;
    private Long orderedQuantity;
    private Long deliveredQuantity;

    public HandoverReportItem toEntity() {
        return HandoverReportItem.builder()
                .itemName(itemName)
                .itemCode(itemCode)
                .unit(unit)
                .orderedQuantity(orderedQuantity)
                .deliveredQuantity(deliveredQuantity)
                .build();
    }

    public static HandoverReportItemDto fromEntity(HandoverReportItem item) {
        if (item == null) {
            return null;
        }
        return HandoverReportItemDto.builder()
                .itemName(item.getItemName())
                .itemCode(item.getItemCode())
                .unit(item.getUnit())
                .orderedQuantity(item.getOrderedQuantity())
                .deliveredQuantity(item.getDeliveredQuantity())
                .build();
    }
}

