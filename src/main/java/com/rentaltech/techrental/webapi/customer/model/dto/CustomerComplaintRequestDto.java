package com.rentaltech.techrental.webapi.customer.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerComplaintRequestDto {

    @NotNull(message = "Cần cung cấp orderId")
    private Long orderId;

    @NotNull(message = "Cần cung cấp deviceId")
    private Long deviceId;

    @NotBlank(message = "Cần mô tả lỗi của thiết bị")
    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    private String customerDescription;
}

