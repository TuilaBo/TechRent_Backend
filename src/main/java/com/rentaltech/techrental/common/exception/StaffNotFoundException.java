package com.rentaltech.techrental.common.exception;

public class StaffNotFoundException extends RuntimeException {
    public StaffNotFoundException(String message) {
        super(message);
    }
    
    public StaffNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
