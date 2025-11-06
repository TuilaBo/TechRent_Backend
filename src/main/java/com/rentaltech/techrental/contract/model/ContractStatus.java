package com.rentaltech.techrental.contract.model;

public enum ContractStatus {
    DRAFT("Nháp"),
    PENDING_ADMIN_SIGNATURE("Chờ admin ký"),
    PENDING_SIGNATURE("Chờ khách hàng ký"),
    SIGNED("Đã ký"),
    ACTIVE("Có hiệu lực"),
    EXPIRED("Hết hạn"),
    TERMINATED("Chấm dứt"),
    CANCELLED("Hủy bỏ");
    
    private final String displayName;
    
    ContractStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

