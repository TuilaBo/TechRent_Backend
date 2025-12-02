package com.rentaltech.techrental.contract.model.dto;

import com.rentaltech.techrental.contract.model.DeviceContractTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@Schema(description = "Thông tin điều khoản hợp đồng áp dụng theo từng thiết bị")
public class DeviceContractTermResponseDto {

    @Schema(description = "ID điều khoản hợp đồng thiết bị")
    Long deviceContractTermId;

    @Schema(description = "Tiêu đề điều khoản")
    String title;

    @Schema(description = "Nội dung chi tiết của điều khoản")
    String content;

    @Schema(description = "Trạng thái kích hoạt của điều khoản")
    boolean active;

    @Schema(description = "ID model thiết bị áp dụng")
    Long deviceModelId;

    @Schema(description = "Tên model thiết bị áp dụng")
    String deviceModelName;

    @Schema(description = "ID danh mục thiết bị áp dụng")
    Long deviceCategoryId;

    @Schema(description = "Tên danh mục thiết bị áp dụng")
    String deviceCategoryName;

    @Schema(description = "Ngày tạo điều khoản")
    LocalDateTime createdAt;

    @Schema(description = "Ngày cập nhật điều khoản")
    LocalDateTime updatedAt;

    public static DeviceContractTermResponseDto from(DeviceContractTerm term) {
        return DeviceContractTermResponseDto.builder()
                .deviceContractTermId(term.getDeviceContractTermId())
                .title(term.getTitle())
                .content(term.getContent())
                .active(Boolean.TRUE.equals(term.getActive()))
                .deviceModelId(term.getDeviceModel() != null ? term.getDeviceModel().getDeviceModelId() : null)
                .deviceModelName(term.getDeviceModel() != null ? term.getDeviceModel().getDeviceName() : null)
                .deviceCategoryId(term.getDeviceCategory() != null ? term.getDeviceCategory().getDeviceCategoryId() : null)
                .deviceCategoryName(term.getDeviceCategory() != null ? term.getDeviceCategory().getDeviceCategoryName() : null)
                .createdAt(term.getCreatedAt())
                .updatedAt(term.getUpdatedAt())
                .build();
    }
}

