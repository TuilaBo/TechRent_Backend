package com.rentaltech.techrental.common.exception;

import com.rentaltech.techrental.common.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "INTERNAL_ERROR",
                "Lỗi hệ thống",
                "Có lỗi xảy ra: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex, WebRequest request) {
        return ResponseUtil.createErrorResponse(
                "UNKNOWN_ERROR",
                "Lỗi không xác định",
                "Có lỗi không mong muốn xảy ra",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}