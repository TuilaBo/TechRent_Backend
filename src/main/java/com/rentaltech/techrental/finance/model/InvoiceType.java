package com.rentaltech.techrental.finance.model;

public enum InvoiceType {
    RENT_PAYMENT("Trả tiền thuê"),
    DEPOSIT_REFUND("Hoàn tiền cọc"),
    COMPENSATION_PAYMENT("Trả tiền đền bù");

    private final String displayName;

    InvoiceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
