package com.rentaltech.techrental.webapi.customer.model;

public enum KYCStatus {
    NOT_STARTED("Chưa bắt đầu"),
    PENDING_VERIFICATION("Đang chờ xác minh"),
    DOCUMENTS_SUBMITTED("Đã nộp giấy tờ"),
    VERIFIED("Đã xác minh"),
    REJECTED("Từ chối"),
    EXPIRED("Hết hạn");

    private final String displayName;

    KYCStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


