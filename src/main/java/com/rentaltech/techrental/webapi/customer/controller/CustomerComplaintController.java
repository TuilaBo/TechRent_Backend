package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintResponseDto;
import com.rentaltech.techrental.webapi.customer.service.CustomerComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/complaints")
@RequiredArgsConstructor
@Tag(name = "Khiếu nại khách hàng", description = "API cho khách hàng quản lý khiếu nại thiết bị")
public class CustomerComplaintController {

    private final CustomerComplaintService complaintService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tạo khiếu nại", description = "Khách hàng tạo khiếu nại về thiết bị bị hỏng trong đơn hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo khiếu nại thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng hoặc thiết bị")
    })
    public ResponseEntity<?> createComplaint(
            @Valid @RequestBody CustomerComplaintRequestDto request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        CustomerComplaintResponseDto response = complaintService.createComplaint(request, username);
        return ResponseUtil.createSuccessResponse(
                "Tạo khiếu nại thành công",
                "Khiếu nại đã được ghi nhận và đang chờ xử lý",
                response,
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Danh sách khiếu nại của tôi", description = "Lấy danh sách tất cả khiếu nại của khách hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> getMyComplaints(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        List<CustomerComplaintResponseDto> complaints = complaintService.getMyComplaints(username);
        return ResponseUtil.createSuccessResponse(
                "Danh sách khiếu nại",
                "",
                complaints,
                HttpStatus.OK
        );
    }

    @GetMapping("/{complaintId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Chi tiết khiếu nại", description = "Xem chi tiết một khiếu nại cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khiếu nại")
    })
    public ResponseEntity<?> getComplaintById(
            @PathVariable Long complaintId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        CustomerComplaintResponseDto response = complaintService.getComplaintById(complaintId, username);
        return ResponseUtil.createSuccessResponse(
                "Chi tiết khiếu nại",
                "",
                response,
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khiếu nại theo đơn hàng", description = "Lấy danh sách khiếu nại của một đơn hàng cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng")
    })
    public ResponseEntity<?> getComplaintsByOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        List<CustomerComplaintResponseDto> complaints = complaintService.getComplaintsByOrder(orderId, username);
        return ResponseUtil.createSuccessResponse(
                "Danh sách khiếu nại theo đơn hàng",
                "",
                complaints,
                HttpStatus.OK
        );
    }
}

