package com.rentaltech.techrental.maintenance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.maintenance.model.MaintenanceItem;
import com.rentaltech.techrental.maintenance.service.MaintenanceItemService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance/items")
@RequiredArgsConstructor
public class MaintenanceItemController {

    private final MaintenanceItemService maintenanceItemService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateItemRequest request) {
        MaintenanceItem data = maintenanceItemService.create(
                request.getDeviceId(),
                request.getDescription(),
                request.getEstimateTime(),
                request.getStatus(),
                request.getOutcomeNote()
        );
        return ResponseUtil.createSuccessResponse("Tạo hạng mục bảo trì thành công", "Đã tạo MaintenanceItem", data, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> listByDevice(@RequestParam("deviceId") Long deviceId) {
        List<MaintenanceItem> data = maintenanceItemService.listByDevice(deviceId);
        return ResponseUtil.createSuccessResponse("Danh sách hạng mục bảo trì", "Theo thiết bị", data, HttpStatus.OK);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable("id") Long id, @RequestBody UpdateStatusRequest request) {
        MaintenanceItem data = maintenanceItemService.updateStatus(id, request.getStatus());
        return ResponseUtil.createSuccessResponse("Cập nhật trạng thái hạng mục bảo trì", "Trạng thái đã được cập nhật", data, HttpStatus.OK);
    }

    @Data
    public static class CreateItemRequest {
        private Long deviceId;
        private String description;
        private Integer estimateTime;
        private String status;
        private String outcomeNote;
    }

    @Data
    public static class UpdateStatusRequest {
        private String status;
    }
}


