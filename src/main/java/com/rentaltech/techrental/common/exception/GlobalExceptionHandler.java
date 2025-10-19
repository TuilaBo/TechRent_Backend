package com.rentaltech.techrental.common.exception;

import com.rentaltech.techrental.common.dto.AuthErrorResponseDto;
import com.rentaltech.techrental.common.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        StringBuilder errorDetails = new StringBuilder();
        errors.forEach((field, message) -> {
            errorDetails.append(field).append(": ").append(message).append("; ");
        });
        
        return ResponseUtil.createErrorResponse(
                "VALIDATION_FAILED",
                "Dữ liệu không hợp lệ",
                errorDetails.toString(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthErrorResponseDto> handleGenericException(Exception ex) {
        return ResponseUtil.createErrorResponse(
                "INTERNAL_ERROR",
                "Lỗi hệ thống",
                "Có lỗi xảy ra: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
