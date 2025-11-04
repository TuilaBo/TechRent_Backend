package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskCategoryUpdateRequestDto {
    
    @NotBlank(message = "Tên category không được để trống")
    @Size(max = 100, message = "Tên category không được quá 100 ký tự")
    private String name;
    
    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String description;
}
