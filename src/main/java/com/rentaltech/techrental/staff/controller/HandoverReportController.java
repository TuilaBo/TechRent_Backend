package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.dto.*;
import com.rentaltech.techrental.staff.service.handover.HandoverReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/staff/handover-reports")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Handover Reports", description = "Nhân viên tạo và quản lý biên bản bàn giao")
public class HandoverReportController {

    private final HandoverReportService handoverReportService;

    @PostMapping(value = "/checkout", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Tạo biên bản bàn giao CHECKOUT", description = "Không nhận discrepancy; nhận danh sách tình trạng thiết bị để lưu snapshot")
    public ResponseEntity<?> createCheckoutReport(
            @Valid @RequestBody HandoverReportCreateOutRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        String username = principal.getUsername();
        HandoverReportResponseDto responseDto =
                handoverReportService.createCheckoutReport(request, username);

        return ResponseUtil.createSuccessResponse(
                "Tạo biên bản bàn giao CHECKOUT thành công",
                "Mã PIN đã được gửi đến email của bạn để ký biên bản",
                responseDto,
                HttpStatus.CREATED
        );
    }

    @PostMapping(value = "/checkin", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Tạo biên bản bàn giao CHECKIN", description = "Nhận discrepancy và xử lý giống hiện tại")
    public ResponseEntity<?> createCheckinReport(
            @Valid @RequestBody HandoverReportCreateInRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        String username = principal.getUsername();
        HandoverReportResponseDto responseDto =
                handoverReportService.createCheckinReport(request, username);

        return ResponseUtil.createSuccessResponse(
                "Tạo biên bản bàn giao CHECKIN thành công",
                "Mã PIN đã được gửi đến email của bạn để ký biên bản",
                responseDto,
                HttpStatus.CREATED
        );
    }

    @PostMapping("/orders/{orderId}/pin")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Gửi PIN cho khách theo đơn hàng", description = "Gửi mã PIN xác nhận cho khách qua SMS/Email dựa trên đơn hàng")
    public ResponseEntity<?> requestPinForOrder(@PathVariable Long orderId) {
        HandoverPinDeliveryDto responseDto = handoverReportService.sendPinForOrder(orderId);
        return ResponseUtil.createSuccessResponse(
                "Gửi PIN thành công",
                "Khách hàng sẽ nhận PIN qua SMS/Email nếu khả dụng",
                responseDto,
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Danh sách biên bản bàn giao", description = "Lấy toàn bộ biên bản bàn giao hiện có")
    public ResponseEntity<?> getAllReports() {
        List<HandoverReportResponseDto> reports = handoverReportService.getAllReports();
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách biên bản thành công",
                "Tất cả biên bản bàn giao hiện có",
                reports,
                HttpStatus.OK
        );
    }

    @GetMapping("/{handoverReportId}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Chi tiết biên bản bàn giao", description = "Xem chi tiết biên bản theo ID")
    public ResponseEntity<?> getReport(@PathVariable Long handoverReportId) {
        HandoverReportResponseDto responseDto = handoverReportService.getReport(handoverReportId);
        return ResponseUtil.createSuccessResponse(
                "Lấy biên bản bàn giao thành công",
                "Chi tiết biên bản bàn giao",
                responseDto,
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Biên bản theo đơn hàng", description = "Liệt kê các biên bản bàn giao gắn với một đơn hàng")
    public ResponseEntity<?> getReportsByOrder(@PathVariable Long orderId) {
        List<HandoverReportResponseDto> reports = handoverReportService.getReportsByOrder(orderId);
        return ResponseUtil.createSuccessResponse(
                "Lấy biên bản theo đơn hàng thành công",
                "Danh sách biên bản bàn giao của đơn hàng",
                reports,
                HttpStatus.OK
        );
    }

    @GetMapping("/technician/{staffId}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Biên bản theo kỹ thuật viên", description = "Liệt kê biên bản do kỹ thuật viên thực hiện")
    public ResponseEntity<?> getReportsByTechnician(@PathVariable Long staffId) {
        List<HandoverReportResponseDto> reports = handoverReportService.getReportsByTechnician(staffId);
        return ResponseUtil.createSuccessResponse(
                "Lấy biên bản theo kỹ thuật viên thành công",
                "Danh sách biên bản bàn giao do kỹ thuật viên thực hiện",
                reports,
                HttpStatus.OK
        );
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Biên bản theo task", description = "Liệt kê biên bản được tạo từ một task cụ thể")
    public ResponseEntity<?> getReportsByTask(@PathVariable Long taskId) {
        List<HandoverReportResponseDto> reports = handoverReportService.getReportsByTask(taskId);
        return ResponseUtil.createSuccessResponse(
                "Lấy biên bản theo nhiệm vụ thành công",
                "Danh sách biên bản bàn giao theo task",
                reports,
                HttpStatus.OK
        );
    }

    @PostMapping("/{handoverReportId}/pin")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Gửi PIN cho nhân viên", description = "Gửi lại mã PIN ký biên bản cho nhân viên phụ trách")
    public ResponseEntity<?> requestPinForStaff(@PathVariable Long handoverReportId) {
        HandoverPinDeliveryDto responseDto = handoverReportService.sendPinToStaffForReport(handoverReportId);
        return ResponseUtil.createSuccessResponse(
                "Gửi PIN thành công",
                "Mã PIN đã được gửi đến email của bạn",
                responseDto,
                HttpStatus.OK
        );
    }

    @PatchMapping("/{handoverReportId}/signature")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Nhân viên ký biên bản", description = "Xác nhận biên bản bàn giao bằng mã PIN của nhân viên")
    public ResponseEntity<?> signByStaff(
            @PathVariable Long handoverReportId,
            @Valid @RequestBody HandoverReportStaffSignRequestDto request) {
        HandoverReportResponseDto responseDto = handoverReportService.signByStaff(handoverReportId, request);
        return ResponseUtil.createSuccessResponse(
                "Ký biên bản thành công",
                "Bạn đã ký biên bản bàn giao",
                responseDto,
                HttpStatus.OK
        );
    }
}

