package com.rentaltech.techrental.contract.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO chứa thông tin khách hàng dùng để ký hợp đồng điện tử")
public class DigitalSignatureRequestDto {
    
    @NotNull(message = "ID hợp đồng không được để trống")
    @Schema(description = "ID hợp đồng cần ký", example = "15")
    private Long contractId;
    
    @NotBlank(message = "Chữ ký điện tử không được để trống")
    @Schema(description = "Chuỗi chữ ký điện tử mã hóa Base64", example = "YmFzZTY0U2lnbmF0dXJl")
    private String digitalSignature;
    
    @NotBlank(message = "Mã PIN không được để trống")
    @Size(min = 6, max = 6, message = "Mã PIN phải có đúng 6 chữ số")
    @Schema(description = "Mã PIN xác thực gồm 6 chữ số", example = "123456")
    private String pinCode;
    
    @Schema(description = "Phương thức ký sử dụng (ví dụ: DIGITAL_CERTIFICATE, SMS_OTP, EMAIL_OTP)")
    private String signatureMethod;
    
    @Schema(description = "Thông tin thiết bị sử dụng để ký nhằm phục vụ bảo mật")
    private String deviceInfo;
    
    @Schema(description = "Địa chỉ IP của thiết bị ký để lưu nhật ký", example = "203.113.135.1")
    private String ipAddress;
}
