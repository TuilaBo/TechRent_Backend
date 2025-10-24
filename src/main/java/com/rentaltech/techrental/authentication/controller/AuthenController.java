package com.rentaltech.techrental.authentication.controller;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.dto.CreateUserRequestDto;
import com.rentaltech.techrental.authentication.model.dto.JWTAuthResponse;
import com.rentaltech.techrental.authentication.model.dto.LoginDto;
import com.rentaltech.techrental.authentication.model.dto.AccountMeResponse;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthenController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AccountService accountService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
            @RequestBody @Valid LoginDto loginDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsernameOrEmail(),
                            loginDto.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = tokenProvider.generateToken(authentication);

            return ResponseEntity.ok(new JWTAuthResponse(token));
            
        } catch (BadCredentialsException e) {
            return ResponseUtil.badCredentials();
            
        } catch (DisabledException e) {
            return ResponseUtil.accountDisabled();
            
        } catch (LockedException e) {
            return ResponseUtil.accountLocked();
            
        } catch (AuthenticationException e) {
            return ResponseUtil.createErrorResponse(
                    "AUTHENTICATION_FAILED",
                    "Xác thực thất bại",
                    "Có lỗi xảy ra trong quá trình xác thực: " + e.getMessage(),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @RequestBody @Valid CreateUserRequestDto request) {
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