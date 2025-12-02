package com.rentaltech.techrental.webapi.admin.controller;

import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.AdminStaffCreateWithAccountRequestDto;
import com.rentaltech.techrental.staff.model.dto.StaffResponseDto;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/staff")
@Tag(name = "Quản lý nhân sự (Admin)", description = "API cho admin tạo và quản lý nhân viên")
public class AdminStaffController {

    @Autowired
    private StaffService staffService;

    
    // Tạo staff + account trong 1 call (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo nhân viên kèm tài khoản", description = "Admin nhập email/password/role để tạo cả account và staff")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo nhân viên thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<StaffResponseDto> createStaffWithAccount(@RequestBody @Valid AdminStaffCreateWithAccountRequestDto request) {
        try {
            Staff savedStaff = staffService.createStaffWithAccount(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(StaffResponseDto.from(savedStaff));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // Lấy tất cả staff (Admin only)
    @GetMapping
    @Operation(summary = "Danh sách nhân viên", description = "Lấy tất cả nhân viên trong hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách nhân viên")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffResponseDto>> getAllStaff() {
        List<Staff> staffList = staffService.getAllStaff();
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(StaffResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Lấy staff theo ID
    @GetMapping("/{staffId}")
    @Operation(summary = "Chi tiết nhân viên", description = "Lấy thông tin nhân viên theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin nhân viên"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ResponseEntity<StaffResponseDto> getStaffById(@PathVariable Long staffId) {
        return staffService.getStaffById(staffId)
                .map(staff -> ResponseEntity.ok(StaffResponseDto.from(staff)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Lấy staff theo Account ID
    @GetMapping("/account/{accountId}")
    @Operation(summary = "Nhân viên theo account ID", description = "Lấy thông tin nhân viên dựa trên account ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin nhân viên"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ResponseEntity<StaffResponseDto> getStaffByAccountId(@PathVariable Long accountId) {
        return staffService.getStaffByAccountId(accountId)
                .map(staff -> ResponseEntity.ok(StaffResponseDto.from(staff)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Lấy staff theo role
    @GetMapping("/role/{staffRole}")
    @Operation(summary = "Nhân viên theo vai trò", description = "Liệt kê nhân viên theo role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách nhân viên")
    })
    public ResponseEntity<List<StaffResponseDto>> getStaffByRole(@PathVariable StaffRole staffRole) {
        List<Staff> staffList = staffService.getStaffByRole(staffRole);
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(StaffResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Lấy active staff
    @GetMapping("/active")
    @Operation(summary = "Nhân viên đang hoạt động", description = "Liệt kê nhân viên đang active")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách nhân viên")
    })
    public ResponseEntity<List<StaffResponseDto>> getActiveStaff() {
        List<Staff> activeStaff = staffService.getActiveStaff();
        List<StaffResponseDto> responseDtos = activeStaff.stream()
                .map(StaffResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Cập nhật trạng thái active/inactive (Admin only)
    @PutMapping("/{staffId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật trạng thái nhân viên", description = "Kích hoạt hoặc vô hiệu hóa nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ResponseEntity<StaffResponseDto> updateStaffStatus(@PathVariable Long staffId, 
                                                             @RequestParam Boolean isActive) {
        try {
            Staff updatedStaff = staffService.updateStaffStatus(staffId, isActive);
            return ResponseEntity.ok(StaffResponseDto.from(updatedStaff));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Cập nhật staff role (Admin only)
    @PutMapping("/{staffId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật vai trò nhân viên", description = "Thay đổi role một nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật role thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ResponseEntity<StaffResponseDto> updateStaffRole(@PathVariable Long staffId, 
                                                           @RequestParam StaffRole staffRole) {
        try {
            Staff updatedStaff = staffService.updateStaffRole(staffId, staffRole);
            return ResponseEntity.ok(StaffResponseDto.from(updatedStaff));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Xóa staff (Admin only - soft delete)
    @DeleteMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa nhân viên", description = "Soft delete nhân viên theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa nhân viên thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ResponseEntity<Void> deleteStaff(@PathVariable Long staffId) {
        try {
            staffService.deleteStaff(staffId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
