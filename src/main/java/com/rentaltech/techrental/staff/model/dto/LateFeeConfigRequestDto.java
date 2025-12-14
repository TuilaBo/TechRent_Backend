package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LateFeeConfigRequestDto {

    @NotNull(message = "Mức phí trả trễ không được bỏ trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Mức phí trả trễ phải lớn hơn hoặc bằng 0")
    private BigDecimal hourlyRate;
}
