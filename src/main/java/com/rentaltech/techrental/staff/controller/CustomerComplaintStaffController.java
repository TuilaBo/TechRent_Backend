package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.customer.model.ComplaintStatus;
import com.rentaltech.techrental.webapi.customer.model.dto.CancelComplaintRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintResponseDto;
import com.rentaltech.techrental.webapi.customer.model.dto.ProcessComplaintRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.ResolveComplaintRequestDto;
import com.rentaltech.techrental.webapi.customer.service.CustomerComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/complaints")
@RequiredArgsConstructor
@Tag(name = "Quản lý khiếu nại (Staff)", description = "API cho staff quản lý khiếu nại khách hàng")
public class CustomerComplaintStaffController {

    private final CustomerComplaintService complaintService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Danh sách khiếu nại", description = "Lấy danh sách tất cả khiếu nại (có thể filter theo status)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> getAllComplaints(
            @Parameter(description = "Filter theo status (PENDING, PROCESSING, RESOLVED, CANCELLED)")
            @RequestParam(required = false) ComplaintStatus status) {
        List<CustomerComplaintResponseDto> complaints = complaintService.getAllComplaints(status);
        return ResponseUtil.createSuccessResponse(
                "Danh sách khiếu nại",
                "",
                complaints,
                HttpStatus.OK
        );
    }

    @GetMapping("/{complaintId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Chi tiết khiếu nại", description = "Xem chi tiết một khiếu nại cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khiếu nại")
    })
    public ResponseEntity<?> getComplaintById(@PathVariable Long complaintId) {
        CustomerComplaintResponseDto response = complaintService.getComplaintById(complaintId);
        return ResponseUtil.createSuccessResponse(
                "Chi tiết khiếu nại",
                "",
                response,
                HttpStatus.OK
        );
    }

    @PostMapping("/{complaintId}/process")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Xử lý khiếu nại", description = "Tự động tìm device thay thế, tạo allocation mới, tạo task cho staff đi đổi máy")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xử lý khiếu nại thành công"),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy device thay thế hoặc dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khiếu nại")
    })
    public ResponseEntity<?> processComplaint(
            @PathVariable Long complaintId,
            @RequestBody(required = false) ProcessComplaintRequestDto request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        String staffNote = request != null ? request.getStaffNote() : null;
        CustomerComplaintResponseDto response = complaintService.processComplaint(complaintId, staffNote, username);
        return ResponseUtil.createSuccessResponse(
                "Xử lý khiếu nại thành công",
                "Đã tìm thấy device thay thế và tạo task cho staff đi đổi máy",
                response,
                HttpStatus.OK
        );
    }

    @PostMapping("/{complaintId}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Đánh dấu đã giải quyết", description = "Đánh dấu khiếu nại đã được giải quyết (sau khi hoàn thành task đổi máy)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đánh dấu giải quyết thành công"),
            @ApiResponse(responseCode = "400", description = "Khiếu nại không ở trạng thái PROCESSING"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khiếu nại")
    })
    public ResponseEntity<?> resolveComplaint(
            @PathVariable Long complaintId,
            @RequestBody(required = false) ResolveComplaintRequestDto request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        String staffNote = request != null ? request.getStaffNote() : null;
        CustomerComplaintResponseDto response = complaintService.resolveComplaint(complaintId, staffNote, username);
        return ResponseUtil.createSuccessResponse(
                "Đánh dấu giải quyết thành công",
                "Khiếu nại đã được đánh dấu là đã giải quyết",
                response,
                HttpStatus.OK
        );
    }

    @PostMapping("/{complaintId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Hủy khiếu nại", description = "Hủy khiếu nại (chỉ ADMIN hoặc OPERATOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hủy khiếu nại thành công"),
            @ApiResponse(responseCode = "400", description = "Không thể hủy khiếu nại ở trạng thái này"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khiếu nại")
    })
    public ResponseEntity<?> cancelComplaint(
            @PathVariable Long complaintId,
            @RequestBody(required = false) CancelComplaintRequestDto request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        String staffNote = request != null ? request.getStaffNote() : null;
        CustomerComplaintResponseDto response = complaintService.cancelComplaint(complaintId, staffNote, username);
        return ResponseUtil.createSuccessResponse(
                "Hủy khiếu nại thành công",
                "Khiếu nại đã được hủy",
                response,
                HttpStatus.OK
        );
    }
}

