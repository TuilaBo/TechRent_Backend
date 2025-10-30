package com.rentaltech.techrental.maintenance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.service.MaintenanceScheduleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance/schedules")
@RequiredArgsConstructor
public class MaintenanceScheduleController {

    private final MaintenanceScheduleService maintenanceScheduleService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateScheduleRequest request) {
        MaintenanceSchedule data = maintenanceScheduleService.createSchedule(
                request.getDeviceId(),
                request.getMaintenancePlanId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus()
        );
        return ResponseUtil.createSuccessResponse("Tạo lịch bảo trì thành công", "Lịch bảo trì đã được tạo", data, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> listByDevice(@RequestParam("deviceId") Long deviceId) {
        List<MaintenanceSchedule> data = maintenanceScheduleService.listByDevice(deviceId);
        return ResponseUtil.createSuccessResponse("Danh sách lịch bảo trì", "Theo thiết bị", data, HttpStatus.OK);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable("id") Long id, @RequestBody UpdateStatusRequest request) {
        MaintenanceSchedule data = maintenanceScheduleService.updateStatus(id, request.getStatus());
        return ResponseUtil.createSuccessResponse("Cập nhật trạng thái lịch bảo trì", "Trạng thái đã được cập nhật", data, HttpStatus.OK);
    }

    @Data
    public static class CreateScheduleRequest {
        private Long deviceId;
        private Long maintenancePlanId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;
        private String status;
    }

    @Data
    public static class UpdateStatusRequest {
        private String status;
    }
}


