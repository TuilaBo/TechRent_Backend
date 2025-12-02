package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Lifecycle thiết bị", description = "Tra cứu lifecycle hiện tại của thiết bị")
public class DeviceLifecycleController {

    private final DeviceRepository deviceRepository;

    @GetMapping("/{id}/lifecycle")
    @Operation(summary = "Xem lifecycle thiết bị", description = "Trả về trạng thái vòng đời hiện tại của thiết bị theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về trạng thái lifecycle"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getLifecycle(@PathVariable("id") Long id) {
        Device device = deviceRepository.findById(id).orElseThrow();
        DeviceLifecycleResponse resp = new DeviceLifecycleResponse();
        resp.setDeviceId(device.getDeviceId());
        resp.setStatus(device.getStatus());
        resp.setLifecycleStage(mapLifecycle(device.getStatus()));
        return ResponseUtil.createSuccessResponse("Trạng thái vòng đời thiết bị", "Thông tin lifecycle cập nhật", resp, HttpStatus.OK);
    }

    private String mapLifecycle(DeviceStatus status) {
        if (status == null) return "KHONG_XAC_DINH";
        switch (status) {
            case AVAILABLE:
                return "TRONG_KHO";
            case PRE_RENTAL_QC:
                return "KIEM_TRA_TRUOC_THUE";
            case RESERVED:
                return "DA_DAT_TRUOC";
            case RENTING:
                return "DANG_THUE";
            case POST_RENTAL_QC:
                return "KIEM_TRA_SAU_THUE";
            case UNDER_MAINTENANCE:
                return "BAO_TRI";
            case DAMAGED:
                return "HONG_HOC";
            case LOST:
                return "THAT_LAC";
            case RETURNED:
                return "DA_TRA";
            case RETIRED:
                return "NGUNG_HOAT_DONG";
            default:
                return "KHONG_XAC_DINH";
        }
    }

    @Data
    public static class DeviceLifecycleResponse {
        private Long deviceId;
        private DeviceStatus status;
        private String lifecycleStage;
    }
}


