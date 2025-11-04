package com.rentaltech.techrental.finance.model;

public enum InvoiceStatus {
    PROCESSING("Đang xử lý"),
    SUCCEEDED("Thành công"),
    FAILED("Thất bại");

    private final String displayName;

    InvoiceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
