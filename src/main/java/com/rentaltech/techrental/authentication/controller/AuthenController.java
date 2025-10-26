package com.rentaltech.techrental.authentication.controller;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.model.dto.AccountMeResponse;
import com.rentaltech.techrental.authentication.model.dto.CreateUserRequestDto;
import com.rentaltech.techrental.authentication.model.dto.JWTAuthResponse;
import com.rentaltech.techrental.authentication.model.dto.LoginDto;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.common.util.ResponseUtil;
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
        try {
            Account account = Account.builder()
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .isActive(false)
                    .role(Role.CUSTOMER)
                    .build();
            Account saved = accountService.addAccount(account);
            accountService.setVerificationCodeAndSendEmail(saved);

            return ResponseUtil.createSuccessResponse(
                    "Đăng ký tài khoản thành công!",
                    "Vui lòng kiểm tra email và xác thực tài khoản để kích hoạt",
                    HttpStatus.CREATED
            );

        } catch (RuntimeException e) {
            return ResponseUtil.createErrorResponse(
                    "REGISTRATION_FAILED",
                    "Đăng ký tài khoản thất bại",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification code", description = "Resend email verification code to the provided email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resent"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<?> resendVerificationCode(@RequestParam("email") String email) {
        try {
            boolean success = accountService.resendVerificationCode(email);
            if (!success) {
                return ResponseUtil.createErrorResponse(
                        "RESEND_FAILED",
                        "Không thể gửi lại mã xác thực",
                        "Email không tồn tại hoặc đã được xác thực",
                        HttpStatus.BAD_REQUEST
                );
            }

            return ResponseUtil.createSuccessResponse(
                    "Gửi lại mã xác thực thành công!",
                    "Mã xác thực mới đã được gửi đến email của bạn",
                    HttpStatus.OK
            );

        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "RESEND_FAILED",
                    "Gửi lại mã xác thực thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
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
        try {
            boolean ok = accountService.verifyEmail(email, code);
            if (!ok) {
                return ResponseUtil.createErrorResponse(
                        "INVALID_VERIFICATION_CODE",
                        "Mã xác thực không hợp lệ hoặc đã hết hạn",
                        "Vui lòng kiểm tra lại mã xác thực hoặc yêu cầu mã mới",
                        HttpStatus.BAD_REQUEST
                );
            }

            return ResponseUtil.createSuccessResponse(
                    "Xác thực email thành công!",
                    "Tài khoản của bạn đã được kích hoạt. Bạn có thể đăng nhập ngay bây giờ",
                    HttpStatus.OK
            );

        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "VERIFICATION_FAILED",
                    "Xác thực email thất bại",
                    "Có lỗi xảy ra trong quá trình xác thực: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
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

        try {
            String username = principal.getUsername();
            return accountService.getByUsername(username)
                    .<ResponseEntity<?>>map(a -> ResponseUtil.createSuccessResponse(
                            "Lấy thông tin tài khoản thành công",
                            "Thông tin tài khoản hiện tại",
                            AccountMeResponse.builder()
                                    .accountId(a.getAccountId())
                                    .username(a.getUsername())
                                    .email(a.getEmail())
                                    .role(a.getRole())
                                    .phoneNumber(a.getPhoneNumber())
                                    .isActive(a.getIsActive())
                                    .build(),
                            HttpStatus.OK
                    ))
                    .orElseGet(() -> ResponseUtil.accountNotFound());
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "INTERNAL_ERROR",
                    "Lỗi hệ thống",
                    "Có lỗi xảy ra khi lấy thông tin tài khoản: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}