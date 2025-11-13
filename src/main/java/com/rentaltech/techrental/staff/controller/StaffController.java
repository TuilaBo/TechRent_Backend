package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.StaffResponseDto;
import com.rentaltech.techrental.staff.model.dto.StaffTaskCompletionStatsDto;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
@Tag(name = "Staff", description = "Staff read APIs")
public class StaffController {

    @Autowired
    private StaffService staffService;

    // Lấy staff theo ID
    @GetMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Get staff by ID", description = "Retrieve staff by ID")
    public ResponseEntity<?> getStaffById(@PathVariable Long staffId) {
        Staff staff = staffService.getStaffByIdOrThrow(staffId);
        return ResponseUtil.createSuccessResponse(
                "Lấy nhân viên thành công",
                "Chi tiết nhân viên",
                mapToResponseDto(staff),
                HttpStatus.OK
        );
    }

    // Lấy staff theo Account ID
    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get staff by account ID", description = "Retrieve staff by account ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<?> getStaffByAccountId(@PathVariable Long accountId) {
        Staff staff = staffService.getStaffByAccountIdOrThrow(accountId);
        return ResponseUtil.createSuccessResponse(
                "Lấy nhân viên theo tài khoản thành công",
                "Chi tiết nhân viên",
                mapToResponseDto(staff),
                HttpStatus.OK
        );
    }

    // Lấy staff theo role (chỉ active staff)
    @GetMapping("/role/{staffRole}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Get staff by role", description = "List staff by role")
    public ResponseEntity<?> getStaffByRole(@PathVariable StaffRole staffRole) {
        List<Staff> staffList = staffService.getStaffByRole(staffRole);
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách nhân viên theo vai trò thành công",
                "Danh sách nhân viên theo vai trò",
                responseDtos,
                HttpStatus.OK
        );
    }

    // Lấy active staff
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Get active staff", description = "List active staff members")
    public ResponseEntity<?> getActiveStaff() {
        List<Staff> activeStaff = staffService.getActiveStaff();
        List<StaffResponseDto> responseDtos = activeStaff.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách nhân viên đang hoạt động thành công",
                "Danh sách nhân viên đang hoạt động",
                responseDtos,
                HttpStatus.OK
        );
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Search staff", description = "Search staff by role and availability within a time window")
    public ResponseEntity<?> searchStaff(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) StaffRole staffRole) {
        List<Staff> staffList = staffService.searchStaff(startTime, endTime, available, staffRole);
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseUtil.createSuccessResponse(
                "Tìm kiếm nhân viên thành công",
                "Danh sách nhân viên phù hợp với tiêu chí",
                responseDtos,
                HttpStatus.OK
        );
    }

    @GetMapping("/performance/completions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Staff completion leaderboard", description = "Thống kê số lượng task hoàn thành theo tháng cho từng nhân viên")
    public ResponseEntity<?> getStaffCompletionLeaderboard(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) StaffRole staffRole) {
        List<StaffTaskCompletionStatsDto> stats = staffService.getStaffCompletionStats(year, month, staffRole);
        return ResponseUtil.createSuccessResponse(
                "Thống kê task hoàn thành thành công",
                "Danh sách nhân viên theo số lượng task hoàn thành",
                stats,
                HttpStatus.OK
        );
    }

    private StaffResponseDto mapToResponseDto(Staff staff) {
        return StaffResponseDto.builder()
                .staffId(staff.getStaffId())
                .accountId(staff.getAccount().getAccountId())
                .username(staff.getAccount().getUsername())
                .email(staff.getAccount().getEmail())
                .phoneNumber(staff.getAccount().getPhoneNumber())
                .staffRole(staff.getStaffRole())
                .isActive(staff.getIsActive())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}
