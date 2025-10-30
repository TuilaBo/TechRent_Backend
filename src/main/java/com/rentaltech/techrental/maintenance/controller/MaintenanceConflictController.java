package com.rentaltech.techrental.maintenance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.maintenance.model.MaintenanceConflict;
import com.rentaltech.techrental.maintenance.model.MaintenanceConflictSource;
import com.rentaltech.techrental.maintenance.service.MaintenanceConflictService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance/conflicts")
@RequiredArgsConstructor
public class MaintenanceConflictController {

    private final MaintenanceConflictService maintenanceConflictService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateConflictRequest request) {
        MaintenanceConflict data = maintenanceConflictService.create(
                request.getMaintenanceScheduleId(),
                request.getBookingCalendarId(),
                request.getStatus(),
                request.getSource()
        );
        return ResponseUtil.createSuccessResponse("Tạo xung đột lịch bảo trì", "MaintenanceConflict created", data, HttpStatus.CREATED);
    }

    @GetMapping("/by-schedule/{scheduleId}")
    public ResponseEntity<?> listBySchedule(@PathVariable("scheduleId") Long scheduleId) {
        List<MaintenanceConflict> data = maintenanceConflictService.listBySchedule(scheduleId);
        return ResponseUtil.createSuccessResponse("Danh sách xung đột theo schedule", "", data, HttpStatus.OK);
    }

    @Data
    public static class CreateConflictRequest {
        private Long maintenanceScheduleId;
        private Long bookingCalendarId;
        private String status;
        private MaintenanceConflictSource source;
    }
}


