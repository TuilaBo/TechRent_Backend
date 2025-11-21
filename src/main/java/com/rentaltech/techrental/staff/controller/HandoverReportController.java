package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.dto.*;
import com.rentaltech.techrental.staff.service.handover.HandoverReportService;
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

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/staff/handover-reports")
@RequiredArgsConstructor
@Validated
@Slf4j
public class HandoverReportController {

    private final HandoverReportService handoverReportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<?> createHandoverReport(
            @RequestPart("data") @Valid HandoverReportCreateRequestDto request,
            @RequestPart(value = "evidences", required = false) List<MultipartFile> evidences,
            @AuthenticationPrincipal UserDetails principal) {
        String username = principal.getUsername();
        HandoverReportResponseDto responseDto =
                handoverReportService.createReport(request, evidences, username);

        return ResponseUtil.createSuccessResponse(
                "Tạo biên bản bàn giao thành công",
                "Mã PIN đã được gửi đến email của bạn để ký biên bản",
                responseDto,
                HttpStatus.CREATED
        );
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<?> createHandoverReportJson(
            @Valid @RequestBody HandoverReportCreateRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        String username = principal.getUsername();
        HandoverReportResponseDto responseDto =
                handoverReportService.createReport(request, Collections.emptyList(), username);

        return ResponseUtil.createSuccessResponse(
                "Tạo biên bản bàn giao thành công",
                "Mã PIN đã được gửi đến email của bạn để ký biên bản",
                responseDto,
                HttpStatus.CREATED
        );
    }

    @PostMapping("/orders/{orderId}/pin")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
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

