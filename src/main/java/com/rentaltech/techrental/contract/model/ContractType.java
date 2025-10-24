package com.rentaltech.techrental.contract.model;

public enum ContractType {
    EQUIPMENT_RENTAL("Thuê thiết bị"),
    SERVICE_CONTRACT("Hợp đồng dịch vụ"),
    MAINTENANCE_CONTRACT("Hợp đồng bảo trì"),
    PURCHASE_CONTRACT("Hợp đồng mua bán"),
    PARTNERSHIP_CONTRACT("Hợp đồng hợp tác");
    
    private final String displayName;
    
    ContractType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

