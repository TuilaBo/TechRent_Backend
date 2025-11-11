package com.rentaltech.techrental.webapi.technician.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportCreateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportUpdateRequestDto;
import com.rentaltech.techrental.webapi.technician.service.QCReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/technician/qc-reports")
@PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
@Tag(name = "QC Reports", description = "Quality control report APIs")
@RequiredArgsConstructor
public class QCReportController {

    private final QCReportService qcReportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create QC report", description = "Technician creates QC report for assigned task")
    public ResponseEntity<?> createReport(@RequestPart("request") @Valid QCReportCreateRequestDto request,
                                          @RequestPart(value = "accessorySnapshot", required = false) MultipartFile accessorySnapshot,
                                          Authentication authentication) {
        String username = authentication.getName();
        var response = qcReportService.createReport(request, accessorySnapshot, username);
        return ResponseUtil.createSuccessResponse(
                "Tạo báo cáo QC thành công!",
                "Báo cáo QC đã được tạo cho task " + response.getTaskId(),
                response,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get QC report", description = "Lấy chi tiết báo cáo QC theo ID")
    public ResponseEntity<?> getReport(@PathVariable Long reportId) {
        var response = qcReportService.getReport(reportId);
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin báo cáo QC thành công!",
                "Thông tin chi tiết báo cáo QC",
                response,
                HttpStatus.OK
        );
    }

    @PutMapping(value = "/{reportId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update QC report", description = "Cập nhật kết quả báo cáo QC")
    public ResponseEntity<?> updateReport(@PathVariable Long reportId,
                                          @RequestPart("request") @Valid QCReportUpdateRequestDto request,
                                          @RequestPart(value = "accessorySnapshot", required = false) MultipartFile accessorySnapshot,
                                          Authentication authentication) {
        String username = authentication.getName();
        var response = qcReportService.updateReport(reportId, request, accessorySnapshot, username);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật báo cáo QC thành công!",
                "Báo cáo QC đã được cập nhật",
                response,
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "List QC reports by order", description = "Lấy báo cáo QC theo đơn thuê")
    public ResponseEntity<?> getReportsByOrder(@PathVariable Long orderId) {
        var response = qcReportService.getReportsByOrder(orderId);
        return ResponseUtil.createSuccessResponse(
                "Lấy báo cáo QC theo đơn hàng thành công!",
                "Danh sách báo cáo QC cho đơn hàng " + orderId,
                response,
                HttpStatus.OK
        );
    }
}
