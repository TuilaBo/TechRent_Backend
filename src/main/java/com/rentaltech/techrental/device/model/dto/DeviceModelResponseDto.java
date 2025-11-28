package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.DeviceModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceModelResponseDto {
    private Long deviceModelId;
    private String deviceName;
    private String description;
    private Long brandId;
    private String imageURL;
    private String specifications;
    private boolean isActive;
    private Long deviceCategoryId;
    private Long amountAvailable;
    private BigDecimal deviceValue;
    private BigDecimal pricePerDay;
    private BigDecimal depositPercent;

    public static DeviceModelResponseDto from(DeviceModel entity) {
        if (entity == null) {
            return null;
        }
        var brand = entity.getBrand();
        var category = entity.getDeviceCategory();
        return DeviceModelResponseDto.builder()
                .deviceModelId(entity.getDeviceModelId())
                .deviceName(entity.getDeviceName())
                .description(entity.getDescription())
                .brandId(brand != null ? brand.getBrandId() : null)
                .imageURL(entity.getImageURL())
                .specifications(entity.getSpecifications())
                .isActive(entity.isActive())
                .deviceCategoryId(category != null ? category.getDeviceCategoryId() : null)
                .amountAvailable(entity.getAmountAvailable())
                .deviceValue(entity.getDeviceValue())
                .pricePerDay(entity.getPricePerDay())
                .depositPercent(entity.getDepositPercent())
                .build();
    }
}
