package com.rentaltech.techrental.maintenance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.maintenance.model.MaintenancePlan;
import com.rentaltech.techrental.maintenance.model.MaintenancePlanScopeType;
import com.rentaltech.techrental.maintenance.model.MaintenanceRuleType;
import com.rentaltech.techrental.maintenance.model.MaintenanceScheduleStatus;
import com.rentaltech.techrental.maintenance.service.MaintenancePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance/plans")
@RequiredArgsConstructor
@Tag(name = "Maintenance Plans", description = "Quản lý kế hoạch bảo trì thiết bị")
public class MaintenancePlanController {

    private final MaintenancePlanService maintenancePlanService;

    @PostMapping
    @Operation(summary = "Tạo kế hoạch bảo trì", description = "Khởi tạo một plan bảo trì với phạm vi và quy tắc cụ thể")
    public ResponseEntity<?> create(@RequestBody CreatePlanRequest request) {
        MaintenancePlan data = maintenancePlanService.createPlan(
                request.getStatus(),
                request.getRuleValue(),
                request.getScopeType(),
                request.getRuleType(),
                request.getActive(),
                request.getDeviceIds(),
                request.getStartDate(),
                request.getEndDate(),
                request.getScheduleStatus()
        );
        return ResponseUtil.createSuccessResponse("Tạo kế hoạch bảo trì", "Plan created", data, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Danh sách kế hoạch bảo trì", description = "Trả về tất cả các kế hoạch bảo trì hiện có")
    public ResponseEntity<?> list() {
        List<MaintenancePlan> data = maintenancePlanService.listPlans();
        return ResponseUtil.createSuccessResponse("Danh sách kế hoạch bảo trì", "", data, HttpStatus.OK);
    }

    @Data
    public static class CreatePlanRequest {
        private String status;
        private Integer ruleValue;
        private MaintenancePlanScopeType scopeType;
        private MaintenanceRuleType ruleType;
        private Boolean active;
        private List<Long> deviceIds;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;
        private MaintenanceScheduleStatus scheduleStatus;
    }
}




