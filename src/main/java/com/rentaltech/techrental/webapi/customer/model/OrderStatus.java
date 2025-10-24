package com.rentaltech.techrental.webapi.customer.model;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    DELIVERY_CONFIRMED,
    DELIVERING,
    RESCHEDULED,
    IN_USE,
    DISPUTED,
    CANCELLED,
    REJECTED,
    COMPLETED,
}