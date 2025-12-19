package com.rentaltech.techrental.webapi.customer.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerUpdateRequestDto {
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được quá 100 ký tự")
    private String email;

    @Size(max = 15, message = "Số điện thoại không được quá 15 ký tự")
    private String phoneNumber;

    @Size(max = 100, message = "Họ tên không được quá 100 ký tự")
    private String fullName;

    @Size(max = 100, message = "Tên ngân hàng không được quá 100 ký tự")
    private String bankName;

    @Size(max = 100, message = "Tên chủ tài khoản không được quá 100 ký tự")
    private String bankAccountHolder;
}

