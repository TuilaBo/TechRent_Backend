package com.rentaltech.techrental.webapi.customer.model;

public enum ComplaintStatus {
    PENDING("Chờ xử lý"),
    PROCESSING("Đang xử lý"),
    RESOLVED("Đã giải quyết"),
    CANCELLED("Đã hủy");

    private final String description;

    ComplaintStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

