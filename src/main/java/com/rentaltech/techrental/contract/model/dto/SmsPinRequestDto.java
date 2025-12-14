package com.rentaltech.techrental.contract.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Thông tin số điện thoại dùng để gửi mã PIN ký hợp đồng")
public class SmsPinRequestDto {
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Số điện thoại không đúng định dạng")
    @Schema(description = "Số điện thoại nhận mã PIN", example = "084912345678")
    private String phoneNumber;
}
