package com.rentaltech.techrental.maintenance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.maintenance.model.MaintenancePlan;
import com.rentaltech.techrental.maintenance.model.MaintenancePlanScopeType;
import com.rentaltech.techrental.maintenance.model.MaintenanceRuleType;
import com.rentaltech.techrental.maintenance.service.MaintenancePlanService;
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
public class MaintenancePlanController {

    private final MaintenancePlanService maintenancePlanService;

    @PostMapping
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
        private String scheduleStatus;
    }
}




