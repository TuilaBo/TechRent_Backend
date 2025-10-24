package com.rentaltech.techrental.common.exception;

public class TaskCreationException extends RuntimeException {
    public TaskCreationException(String message) {
        super(message);
    }
    
    public TaskCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
