package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.StaffResponseDto;
import com.rentaltech.techrental.staff.model.dto.StaffTaskCompletionStatsDto;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Nhân sự", description = "API tra cứu nhân viên và thống kê hiệu suất")
public class StaffController {

    @Autowired
    private StaffService staffService;

    // Lấy staff theo ID
    @GetMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Chi tiết nhân viên", description = "Lấy thông tin nhân viên theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin nhân viên"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên tương ứng"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getStaffById(@PathVariable Long staffId) {
        Staff staff = staffService.getStaffByIdOrThrow(staffId);
        return ResponseUtil.createSuccessResponse(
                "Lấy nhân viên thành công",
                "Chi tiết nhân viên",
                StaffResponseDto.from(staff),
                HttpStatus.OK
        );
    }

    // Lấy staff theo Account ID
    @GetMapping("/account/{accountId}")
    @Operation(summary = "Lấy nhân viên theo tài khoản", description = "Lấy thông tin nhân viên dựa trên account ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin nhân viên"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên tương ứng"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<?> getStaffByAccountId(@PathVariable Long accountId) {
        Staff staff = staffService.getStaffByAccountIdOrThrow(accountId);
        return ResponseUtil.createSuccessResponse(
                "Lấy nhân viên theo tài khoản thành công",
                "Chi tiết nhân viên",
                StaffResponseDto.from(staff),
                HttpStatus.OK
        );
    }

    // Lấy staff theo role (chỉ active staff)
    @GetMapping("/role/{staffRole}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Nhân viên theo vai trò", description = "Liệt kê nhân viên đang hoạt động theo vai trò cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách nhân viên"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getStaffByRole(@PathVariable StaffRole staffRole) {
        List<Staff> staffList = staffService.getStaffByRole(staffRole);
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(StaffResponseDto::from)
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
    @Operation(summary = "Danh sách nhân viên đang hoạt động", description = "Liệt kê tất cả nhân viên đang active")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách nhân viên đang hoạt động"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getActiveStaff() {
        List<Staff> activeStaff = staffService.getActiveStaff();
        List<StaffResponseDto> responseDtos = activeStaff.stream()
                .map(StaffResponseDto::from)
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
    @Operation(summary = "Tìm kiếm nhân viên", description = "Tìm nhân viên theo vai trò và khoảng thời gian rảnh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách nhân viên phù hợp"),
            @ApiResponse(responseCode = "500", description = "Không thể tìm kiếm do lỗi hệ thống")
    })
    public ResponseEntity<?> searchStaff(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) StaffRole staffRole) {
        List<Staff> staffList = staffService.searchStaff(startTime, endTime, available, staffRole);
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(StaffResponseDto::from)
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
    @Operation(summary = "Bảng xếp hạng hoàn thành task", 
               description = "Thống kê số lượng task hoàn thành theo tháng cho từng nhân viên, hỗ trợ phân trang và sắp xếp. " +
                             "Có thể sort theo: completedTaskCount, username, email, phoneNumber, staffRole, staffId, accountId. " +
                             "Format: sort=field,direction (ví dụ: sort=completedTaskCount,desc hoặc sort=username,asc)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thống kê hoàn thành task"),
            @ApiResponse(responseCode = "500", description = "Không thể thống kê do lỗi hệ thống")
    })
    public ResponseEntity<?> getStaffCompletionLeaderboard(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) StaffRole staffRole,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) String sort) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                parseSort(sort)
        );
        org.springframework.data.domain.Page<StaffTaskCompletionStatsDto> stats = staffService.getStaffCompletionStats(year, month, staffRole, pageable);
        return ResponseUtil.createSuccessResponse(
                "Thống kê task hoàn thành thành công",
                "Danh sách nhân viên theo số lượng task hoàn thành",
                stats,
                HttpStatus.OK
        );
    }

    private org.springframework.data.domain.Sort parseSort(String sort) {
        if (sort == null || sort.isEmpty()) {
            return org.springframework.data.domain.Sort.by("completedTaskCount").descending();
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toUpperCase() : "DESC";
        org.springframework.data.domain.Sort.Direction sortDirection = 
                "ASC".equals(direction) ? org.springframework.data.domain.Sort.Direction.ASC 
                                        : org.springframework.data.domain.Sort.Direction.DESC;
        return org.springframework.data.domain.Sort.by(sortDirection, property);
    }

}
