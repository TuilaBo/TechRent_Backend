package com.rentaltech.techrental.contract.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Thông tin kết quả ký hợp đồng điện tử")
public class DigitalSignatureResponseDto {
    @Schema(description = "ID bản ghi chữ ký")
    private Long signatureId;

    @Schema(description = "ID hợp đồng đã ký")
    private Long contractId;

    @Schema(description = "Chuỗi băm SHA-256 của chữ ký")
    private String signatureHash;

    @Schema(description = "Phương thức ký được sử dụng")
    private String signatureMethod;

    @Schema(description = "Thông tin thiết bị đã ký")
    private String deviceInfo;

    @Schema(description = "Địa chỉ IP của thiết bị ký", example = "203.113.135.1")
    private String ipAddress;

    @Schema(description = "Thời điểm ký")
    private LocalDateTime signedAt;

    @Schema(description = "Trạng thái chữ ký (VALID, INVALID, EXPIRED)")
    private String signatureStatus;

    @Schema(description = "Thông tin chứng thư số nếu có")
    private String certificateInfo;

    @Schema(description = "Chuỗi nhật ký kiểm tra chữ ký")
    private String auditTrail;
}
