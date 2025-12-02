package com.rentaltech.techrental.authentication.controller;

import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.model.dto.AccountMeResponse;
import com.rentaltech.techrental.authentication.model.dto.CreateUserRequestDto;
import com.rentaltech.techrental.authentication.model.dto.JWTAuthResponse;
import com.rentaltech.techrental.authentication.model.dto.LoginDto;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthenController {

    private final AccountService accountService;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with username or email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "423", description = "Account locked"),
            @ApiResponse(responseCode = "403", description = "Account disabled")
    })
    public ResponseEntity<?> authenticateUser(@RequestBody @Valid LoginDto loginDto) {
        String token = accountService.authenticateAndGenerateToken(loginDto);
        return ResponseEntity.ok(new JWTAuthResponse(token));
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register a new customer account and send verification email")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<?> registerUser(@RequestBody @Valid CreateUserRequestDto request) {
        accountService.registerCustomer(request);
        return ResponseUtil.createSuccessResponse(
                "Đăng ký tài khoản thành công!",
                "Vui lòng kiểm tra email và xác thực tài khoản để kích hoạt",
                HttpStatus.CREATED
        );
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification code", description = "Resend email verification code to the provided email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resent"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<?> resendVerificationCode(@RequestParam("email") String email) {
        accountService.resendVerificationCode(email);
        return ResponseUtil.createSuccessResponse(
                "Gửi lại mã xác thực thành công!",
                "Mã xác thực mới đã được gửi đến email của bạn",
                HttpStatus.OK
        );
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify email using verification code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verified"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired code")
    })
    public ResponseEntity<?> verifyEmail(
            @RequestParam("email") String email,
            @RequestParam("code") String code) {
        accountService.verifyEmail(email, code);
        return ResponseUtil.createSuccessResponse(
                "Xác thực email thành công!",
                "Tài khoản của bạn đã được kích hoạt. Bạn có thể đăng nhập ngay bây giờ",
                HttpStatus.OK
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Get current account", description = "Get profile of the authenticated account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> getMe(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        if (principal == null) {
            return ResponseUtil.unauthorized();
        }

        String username = principal.getUsername();
        return accountService.getByUsername(username)
                .<ResponseEntity<?>>map(a -> {
                    Long customerId = null;
                    Long staffId = null;

                    if (a.getRole() == Role.CUSTOMER) {
                        customerId = customerRepository.findByAccount_AccountId(a.getAccountId())
                                .map(c -> c.getCustomerId())
                                .orElse(null);
                    } else {
                        staffId = staffRepository.findByAccount_AccountId(a.getAccountId())
                                .map(s -> s.getStaffId())
                                .orElse(null);
                    }

                    return ResponseUtil.createSuccessResponse(
                        "Lấy thông tin tài khoản thành công",
                        "Thông tin tài khoản hiện tại",
                        AccountMeResponse.builder()
                                .accountId(a.getAccountId())
                                .username(a.getUsername())
                                .email(a.getEmail())
                                .role(a.getRole())
                                .phoneNumber(a.getPhoneNumber())
                                .isActive(a.getIsActive())
                                .customerId(customerId)
                                .staffId(staffId)
                                .build(),
                        HttpStatus.OK
                );
                })
                .orElseGet(ResponseUtil::accountNotFound);
    }
}
