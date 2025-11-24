package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Device Lifecycle", description = "Tra cứu lifecycle hiện tại của thiết bị")
public class DeviceLifecycleController {

    private final DeviceRepository deviceRepository;

    @GetMapping("/{id}/lifecycle")
    @Operation(summary = "Xem lifecycle thiết bị", description = "Trả về trạng thái lifecycle hiện tại của thiết bị theo ID")
    public ResponseEntity<?> getLifecycle(@PathVariable("id") Long id) {
        Device device = deviceRepository.findById(id).orElseThrow();
        DeviceLifecycleResponse resp = new DeviceLifecycleResponse();
        resp.setDeviceId(device.getDeviceId());
        resp.setStatus(device.getStatus());
        resp.setLifecycleStage(mapLifecycle(device.getStatus()));
        return ResponseUtil.createSuccessResponse("Thiết bị lifecycle", "Lifecycle hiện tại", resp, HttpStatus.OK);
    }

    private String mapLifecycle(DeviceStatus status) {
        if (status == null) return "UNKNOWN";
        switch (status) {
            case AVAILABLE:
                return "INVENTORY";
            case PRE_RENTAL_QC:
                return "PREP_QC";
            case RESERVED:
                return "RESERVED";
            case RENTING:
                return "IN_USE";
            case POST_RENTAL_QC:
                return "RETURN_QC";
            case UNDER_MAINTENANCE:
                return "MAINTENANCE";
            case DAMAGED:
                return "DAMAGED";
            case LOST:
                return "LOST";
            case RETURNED:
                return "RETURNED";
            case RETIRED:
                return "RETIRED";
            default:
                return "UNKNOWN";
        }
    }

    @Data
    public static class DeviceLifecycleResponse {
        private Long deviceId;
        private DeviceStatus status;
        private String lifecycleStage;
    }
}


