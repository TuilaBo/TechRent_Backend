package com.rentaltech.techrental.contract.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Thông tin email dùng để gửi mã PIN ký hợp đồng hoặc phụ lục")
public class EmailPinRequestDto {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Schema(description = "Địa chỉ email nhận mã PIN", example = "khachhang@techrent.vn")
    private String email;
}
