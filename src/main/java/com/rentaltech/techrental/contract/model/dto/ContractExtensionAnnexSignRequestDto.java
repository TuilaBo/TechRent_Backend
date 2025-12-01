package com.rentaltech.techrental.contract.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Thông tin chữ ký dùng để ký phụ lục gia hạn")
public class ContractExtensionAnnexSignRequestDto {

    @NotBlank
    @Schema(description = "Chuỗi chữ ký điện tử dạng Base64")
    private String digitalSignature;

    @NotBlank
    @Schema(description = "Phương thức ký (DIGITAL_CERTIFICATE, SMS_OTP, EMAIL_OTP)")
    private String signatureMethod;

    @Schema(description = "Thông tin thiết bị phục vụ kiểm tra bảo mật")
    private String deviceInfo;

    @Schema(description = "Địa chỉ IP của thiết bị ký")
    private String ipAddress;

    @Schema(description = "Mã PIN xác thực phụ lục (nếu bắt buộc)", example = "654321")
    private String pinCode;
}
