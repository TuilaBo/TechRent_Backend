package com.rentaltech.techrental.maintenance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.model.dto.*;
import com.rentaltech.techrental.maintenance.service.MaintenanceScheduleService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance/schedules")
@RequiredArgsConstructor
public class MaintenanceScheduleController {

    private final MaintenanceScheduleService maintenanceScheduleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ResponseEntity<?> create(@RequestBody @Valid CreateScheduleRequest request) {
        MaintenanceSchedule data = maintenanceScheduleService.createSchedule(
                request.getDeviceId(),
                request.getMaintenancePlanId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus()
        );
        return ResponseUtil.createSuccessResponse("Tạo lịch bảo trì thành công", "Lịch bảo trì đã được tạo", data, HttpStatus.CREATED);
    }

    @PostMapping("/by-category")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ResponseEntity<?> createByCategory(@RequestBody @Valid MaintenanceScheduleByCategoryRequestDto request) {
        List<MaintenanceSchedule> data = maintenanceScheduleService.createSchedulesByCategory(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo lịch bảo trì theo category thành công",
                "Đã tạo " + data.size() + " lịch bảo trì",
                data,
                HttpStatus.CREATED
        );
    }

    @PostMapping("/by-usage")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ResponseEntity<?> createByUsage(@RequestBody @Valid MaintenanceScheduleByUsageRequestDto request) {
        List<MaintenanceSchedule> data = maintenanceScheduleService.createSchedulesByUsage(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo lịch bảo trì theo số lần sử dụng thành công",
                "Đã tạo " + data.size() + " lịch bảo trì",
                data,
                HttpStatus.CREATED
        );
    }

    @PostMapping("/check-conflicts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ResponseEntity<?> checkConflicts(@RequestBody @Valid MaintenanceConflictCheckRequestDto request) {
        List<MaintenanceConflictResponseDto> conflicts = maintenanceScheduleService.checkConflicts(request);
        return ResponseUtil.createSuccessResponse(
                conflicts.isEmpty() ? "Không có xung đột" : "Phát hiện xung đột lịch bảo trì",
                conflicts.isEmpty() ? "Tất cả thiết bị đều sẵn sàng" : "Có " + conflicts.size() + " thiết bị bị xung đột",
                conflicts,
                HttpStatus.OK
        );
    }

    @GetMapping("/priority")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ResponseEntity<?> getPriorityMaintenanceDevices() {
        List<PriorityMaintenanceDeviceDto> devices = maintenanceScheduleService.getPriorityMaintenanceDevices();
        return ResponseUtil.createSuccessResponse(
                "Danh sách thiết bị cần ưu tiên bảo trì",
                "Có " + devices.size() + " thiết bị cần bảo trì",
                devices,
                HttpStatus.OK
        );
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ResponseEntity<?> getActiveMaintenanceSchedules() {
        List<MaintenanceSchedule> data = maintenanceScheduleService.getActiveMaintenanceSchedules();
        return ResponseUtil.createSuccessResponse(
                "Danh sách thiết bị đang bảo trì",
                "Có " + data.size() + " thiết bị đang bảo trì",
                data,
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ResponseEntity<?> listByDevice(@RequestParam("deviceId") Long deviceId) {
        List<MaintenanceSchedule> data = maintenanceScheduleService.listByDevice(deviceId);
        return ResponseUtil.createSuccessResponse("Danh sách lịch bảo trì", "Theo thiết bị", data, HttpStatus.OK);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
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


