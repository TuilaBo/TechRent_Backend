package com.rentaltech.techrental.maintenance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.maintenance.model.MaintenanceResult;
import com.rentaltech.techrental.maintenance.service.MaintenanceResultService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance/results")
@RequiredArgsConstructor
public class MaintenanceResultController {

    private final MaintenanceResultService maintenanceResultService;

    @PostMapping
    public ResponseEntity<?> upsert(@RequestBody UpsertResultRequest request) {
        MaintenanceResult data = maintenanceResultService.upsert(
                request.getMaintenanceItemId(),
                request.getMaintenanceScheduleId(),
                request.getResult(),
                request.getStatus()
        );
        return ResponseUtil.createSuccessResponse("Lưu kết quả bảo trì", "Upsert MaintenanceResult", data, HttpStatus.OK);
    }

    @GetMapping("/by-item/{itemId}")
    public ResponseEntity<?> listByItem(@PathVariable("itemId") Long itemId) {
        List<MaintenanceResult> data = maintenanceResultService.listByItem(itemId);
        return ResponseUtil.createSuccessResponse("Danh sách kết quả theo item", "", data, HttpStatus.OK);
    }

    @GetMapping("/by-schedule/{scheduleId}")
    public ResponseEntity<?> listBySchedule(@PathVariable("scheduleId") Long scheduleId) {
        List<MaintenanceResult> data = maintenanceResultService.listBySchedule(scheduleId);
        return ResponseUtil.createSuccessResponse("Danh sách kết quả theo schedule", "", data, HttpStatus.OK);
    }

    @Data
    public static class UpsertResultRequest {
        private Long maintenanceItemId;
        private Long maintenanceScheduleId;
        private String result;
        private String status;
    }
}


