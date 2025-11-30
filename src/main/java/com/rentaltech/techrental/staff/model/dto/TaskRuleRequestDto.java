package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskRuleRequestDto {

    @NotBlank(message = "Tên rule không được để trống")
    private String name;

    private String description;

    @Positive(message = "Số công việc tối đa phải lớn hơn 0")
    private Integer maxTasksPerDay;

    @NotNull(message = "Trạng thái active không được để trống")
    private Boolean active;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;
}

