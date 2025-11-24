package com.rentaltech.techrental.staff.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceQualityInfo {

    @Column(name = "device_serial_number", nullable = false, length = 100)
    private String deviceSerialNumber;

    @Column(name = "quality_status", length = 50)
    private String qualityStatus; // GOOD, MINOR_DAMAGE, MODERATE_DAMAGE, MAJOR_DAMAGE

    @Column(name = "quality_description", length = 1000)
    private String qualityDescription; // Ví dụ: "Bị hư màn hình nhẹ", "Vỏ máy có vết xước nhỏ"

    @Column(name = "device_model_name", length = 255)
    private String deviceModelName;
}


