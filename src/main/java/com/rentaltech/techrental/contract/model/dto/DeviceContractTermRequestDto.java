package com.rentaltech.techrental.contract.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Thông tin yêu cầu để tạo hoặc cập nhật điều khoản hợp đồng cho thiết bị")
public class DeviceContractTermRequestDto {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Tiêu đề điều khoản thiết bị", example = "Điều khoản bảo hành thiết bị")
    private String title;

    @NotBlank
    @Schema(description = "Nội dung chi tiết của điều khoản")
    private String content;

    @Schema(description = "ID model thiết bị áp dụng")
    private Long deviceModelId;

    @Schema(description = "ID nhóm loại thiết bị áp dụng")
    private Long deviceCategoryId;

    @Schema(description = "Trạng thái kích hoạt của điều khoản")
    private Boolean active = Boolean.TRUE;
}
