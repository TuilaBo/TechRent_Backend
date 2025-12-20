package com.rentaltech.techrental.common.exception;

import com.rentaltech.techrental.common.util.ResponseUtil;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<?> handleTaskNotFoundException(TaskNotFoundException ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "TASK_NOT_FOUND",
                "Không tìm thấy công việc",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    // ===== Authentication exceptions (moved from AuthenController.authenticateUser) =====
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex) {
        return ResponseUtil.badCredentials();
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<?> handleDisabled(DisabledException ex) {
        return ResponseUtil.accountDisabled();
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<?> handleLocked(LockedException ex) {
        return ResponseUtil.accountLocked();
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthException(AuthenticationException ex) {
        return ResponseUtil.createErrorResponse(
                "AUTHENTICATION_FAILED",
                "Xác thực thất bại",
                "Có lỗi xảy ra trong quá trình xác thực: " + ex.getMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> handleAuthException(AuthorizationDeniedException ex) {
        return ResponseUtil.createErrorResponse(
                "AUTHORIZATION_FAILED",
                "Xác thực thất bại",
                "Bạn không thể thực hiện hành động này: " + ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseUtil.createErrorResponse(
                "ACCESS_DENIED",
                "Không có quyền truy cập",
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(TaskCreationException.class)
    public ResponseEntity<?> handleTaskCreationException(TaskCreationException ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "TASK_CREATION_FAILED",
                "Tạo công việc thất bại",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(StaffNotFoundException.class)
    public ResponseEntity<?> handleStaffNotFoundException(StaffNotFoundException ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "STAFF_NOT_FOUND",
                "Không tìm thấy nhân viên",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "INVALID_ARGUMENT",
                "Tham số không hợp lệ",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNoSuchElementException(NoSuchElementException ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "NOT_FOUND",
                "Không tìm thấy",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "INVALID_STATE",
                "Trạng thái không hợp lệ",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException ex) {
        String details = ex.getConstraintViolations().isEmpty()
                ? ex.getMessage()
                : ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseUtil.createErrorResponse(
                "VALIDATION_FAILED",
                "Dữ liệu không hợp lệ",
                details,
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (details.isEmpty()) {
            details = ex.getMessage();
        }
        return ResponseUtil.createErrorResponse(
                "VALIDATION_FAILED",
                "Dữ liệu không hợp lệ",
                details,
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, WebRequest request) {
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = ex.getClass().getSimpleName() + " occurred";
        }
        // Log full stack trace for debugging
        System.err.println("RuntimeException: " + message);
        ex.printStackTrace();
        return ResponseUtil.createErrorResponse(
                "INTERNAL_ERROR",
                "Lỗi hệ thống",
                "Có lỗi xảy ra: " + message,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "PAYLOAD_TOO_LARGE",
                "Tệp tải lên vượt quá giới hạn",
                "Vui lòng chọn tệp dung lượng nhỏ hơn",
                HttpStatus.PAYLOAD_TOO_LARGE
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "UNKNOWN_ERROR",
                "Lỗi không xác định",
                "Có lỗi không mong muốn xảy ra: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
