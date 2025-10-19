package com.rentaltech.techrental.common.util;

import com.rentaltech.techrental.common.dto.AuthErrorResponseDto;
import com.rentaltech.techrental.common.dto.SuccessResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtil {

    public static ResponseEntity<AuthErrorResponseDto> createErrorResponse(
            String error, String message, String details, HttpStatus status) {
        AuthErrorResponseDto errorResponse = AuthErrorResponseDto.builder()
                .error(error)
                .message(message)
                .details(details)
                .status(status.value())
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }

    public static ResponseEntity<SuccessResponseDto> createSuccessResponse(
            String message, String details, HttpStatus status) {
        SuccessResponseDto successResponse = SuccessResponseDto.builder()
                .status("SUCCESS")
                .message(message)
                .details(details)
                .code(status.value())
                .build();
        return ResponseEntity.status(status).body(successResponse);
    }

    public static ResponseEntity<SuccessResponseDto> createSuccessResponse(
            String message, String details, Object data, HttpStatus status) {
        SuccessResponseDto successResponse = SuccessResponseDto.builder()
                .status("SUCCESS")
                .message(message)
                .details(details)
                .code(status.value())
                .data(data)
                .build();
        return ResponseEntity.status(status).body(successResponse);
    }

    // Common error responses
    public static ResponseEntity<AuthErrorResponseDto> badCredentials() {
        return createErrorResponse(
                "INVALID_CREDENTIALS",
                "Tên đăng nhập hoặc mật khẩu không đúng",
                "Vui lòng kiểm tra lại thông tin đăng nhập",
                HttpStatus.UNAUTHORIZED
        );
    }

    public static ResponseEntity<AuthErrorResponseDto> accountDisabled() {
        return createErrorResponse(
                "ACCOUNT_DISABLED",
                "Tài khoản chưa được kích hoạt",
                "Vui lòng kiểm tra email và xác thực tài khoản trước khi đăng nhập",
                HttpStatus.FORBIDDEN
        );
    }

    public static ResponseEntity<AuthErrorResponseDto> accountLocked() {
        return createErrorResponse(
                "ACCOUNT_LOCKED",
                "Tài khoản đã bị khóa",
                "Tài khoản của bạn đã bị khóa do vi phạm chính sách. Vui lòng liên hệ admin",
                HttpStatus.FORBIDDEN
        );
    }

    public static ResponseEntity<AuthErrorResponseDto> unauthorized() {
        return createErrorResponse(
                "UNAUTHORIZED",
                "Chưa đăng nhập",
                "Vui lòng đăng nhập để truy cập thông tin tài khoản",
                HttpStatus.UNAUTHORIZED
        );
    }

    public static ResponseEntity<AuthErrorResponseDto> accountNotFound() {
        return createErrorResponse(
                "ACCOUNT_NOT_FOUND",
                "Không tìm thấy tài khoản",
                "Tài khoản của bạn không tồn tại trong hệ thống",
                HttpStatus.NOT_FOUND
        );
    }
}
