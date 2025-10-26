package com.rentaltech.techrental.contract.model.dto;

import com.rentaltech.techrental.contract.model.ContractType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request DTO để tạo hợp đồng mới")
public class ContractCreateRequestDto {
    
    @Schema(description = "Tiêu đề hợp đồng", example = "Hợp đồng thuê laptop Dell")
    @NotBlank(message = "Tiêu đề hợp đồng không được để trống")
    @Size(max = 200, message = "Tiêu đề không được quá 200 ký tự")
    private String title;
    
    @Schema(description = "Mô tả hợp đồng", example = "Thuê laptop Dell cho dự án phát triển phần mềm")
    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String description;
    
    @Schema(description = "Loại hợp đồng", example = "EQUIPMENT_RENTAL")
    @NotNull(message = "Loại hợp đồng không được để trống")
    private ContractType contractType;
    
    @Schema(description = "ID khách hàng", example = "6")
    @NotNull(message = "ID khách hàng không được để trống")
    @Positive(message = "ID khách hàng phải là số dương")
    private Long customerId;
    
    @Schema(description = "ID đơn thuê (optional)", example = "1")
    private Long orderId;
    
    @Schema(description = "Nội dung hợp đồng", example = "Điều khoản và điều kiện hợp đồng thuê thiết bị")
    @Size(max = 10000, message = "Nội dung hợp đồng không được quá 10000 ký tự")
    private String contractContent;
    
    @Schema(description = "Điều khoản và điều kiện", example = "Khách hàng có trách nhiệm bảo quản thiết bị")
    @Size(max = 5000, message = "Điều khoản không được quá 5000 ký tự")
    private String termsAndConditions;
    
    @Schema(description = "Số ngày thuê", example = "90")
    @Min(value = 1, message = "Thời gian thuê phải ít nhất 1 ngày")
    @Max(value = 3650, message = "Thời gian thuê không được quá 10 năm")
    private Integer rentalPeriodDays;
    
    @Schema(description = "Tổng tiền", example = "3000000.00")
    @DecimalMin(value = "0.0", message = "Tổng tiền không được âm")
    @Digits(integer = 13, fraction = 2, message = "Tổng tiền không hợp lệ")
    private BigDecimal totalAmount;
    
    @Schema(description = "Tiền cọc", example = "600000.00")
    @DecimalMin(value = "0.0", message = "Tiền cọc không được âm")
    @Digits(integer = 13, fraction = 2, message = "Tiền cọc không hợp lệ")
    private BigDecimal depositAmount;
    
    @Schema(description = "Ngày bắt đầu", example = "2025-10-20T00:00:00")
    @Future(message = "Ngày bắt đầu phải là ngày tương lai")
    private LocalDateTime startDate;
    
    @Schema(description = "Ngày kết thúc", example = "2026-01-20T00:00:00")
    @Future(message = "Ngày kết thúc phải là ngày tương lai")
    private LocalDateTime endDate;
    
    @Schema(description = "Ngày hết hạn", example = "2025-10-25T00:00:00")
    @Future(message = "Ngày hết hạn phải là ngày tương lai")
    private LocalDateTime expiresAt;
}
