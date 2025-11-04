package com.rentaltech.techrental.finance.model;

public enum PaymentMethod {
    PAYOS("PayOS"),
    MOMO("MoMo"),
    BANK_ACCOUNT("Bank Account");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
