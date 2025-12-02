package com.rentaltech.techrental.authentication.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "Account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Username không được để trống")
    @Size(min = 6, max = 50, message = "Username phải từ 6 đến 50 ký tự")
    private String username;

    @Column(name = "password", nullable = false)
    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, message = "Password phải có ít nhất 6 ký tự")
    private String password;


    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;


    @Column(name = "email", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;


    @Column(name = "phone_number", length = 15, unique = true)
    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;


    @Column(name = "verification_code", length = 6)
    private String verificationCode;

    @Column(name = "verification_expiry")
    private java.time.LocalDateTime verificationExpiry;

    @Column(name = "reset_password_code", length = 6)
    private String resetPasswordCode;

    @Column(name = "reset_password_expiry")
    private java.time.LocalDateTime resetPasswordExpiry;

}
