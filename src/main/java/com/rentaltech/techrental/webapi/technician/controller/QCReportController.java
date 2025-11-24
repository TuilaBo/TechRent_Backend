package com.rentaltech.techrental.webapi.technician.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPostRentalCreateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPreRentalCreateRequestDto;
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
@Tag(name = "QC Reports", description = "Quality control report APIs")
@RequiredArgsConstructor
public class QCReportController {

    private final QCReportService qcReportService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @PostMapping(value = "/pre-rental", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create PRE-RENTAL QC report", description = "Không nhận discrepancies, dùng cho giai đoạn PRE_RENTAL")
    public ResponseEntity<?> createPreRentalReport(@RequestPart("request") @Valid QCReportPreRentalCreateRequestDto request,
                                                   @RequestPart(value = "accessorySnapshot", required = false) MultipartFile accessorySnapshot,
                                                   Authentication authentication) {
        String username = authentication.getName();
        var response = qcReportService.createPreRentalReport(request, accessorySnapshot, username);
        return ResponseUtil.createSuccessResponse(
                "Tạo báo cáo QC PRE_RENTAL thành công!",
                "Báo cáo QC đã được tạo cho task " + response.getTaskId(),
                response,
                HttpStatus.CREATED
        );
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @PostMapping(value = "/post-rental", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create POST-RENTAL QC report", description = "Cho phép gửi discrepancies như hiện tại")
    public ResponseEntity<?> createPostRentalReport(@RequestPart("request") @Valid QCReportPostRentalCreateRequestDto request,
                                                    @RequestPart(value = "accessorySnapshot", required = false) MultipartFile accessorySnapshot,
                                                    Authentication authentication) {
        String username = authentication.getName();
        var response = qcReportService.createPostRentalReport(request, accessorySnapshot, username);
        return ResponseUtil.createSuccessResponse(
                "Tạo báo cáo QC POST_RENTAL thành công!",
                "Báo cáo QC đã được tạo cho task " + response.getTaskId(),
                response,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
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
