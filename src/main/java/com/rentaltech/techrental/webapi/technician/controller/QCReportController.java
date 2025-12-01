package com.rentaltech.techrental.webapi.technician.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPostRentalCreateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPostRentalUpdateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPreRentalCreateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPreRentalUpdateRequestDto;
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
@Tag(name = "Báo cáo QC", description = "API dành cho kỹ thuật viên tạo/cập nhật báo cáo kiểm định thiết bị")
@RequiredArgsConstructor
public class QCReportController {

    private final QCReportService qcReportService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @PostMapping(value = "/pre-rental", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tạo báo cáo QC PRE-RENTAL", description = "Không nhận discrepancy, dùng cho giai đoạn kiểm tra trước thuê")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo báo cáo QC thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu báo cáo không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Không thể tạo báo cáo do lỗi hệ thống")
    })
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
    @Operation(summary = "Tạo báo cáo QC POST-RENTAL", description = "Cho phép gửi discrepancy khi kiểm tra sau thuê")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo báo cáo QC thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu báo cáo không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Không thể tạo báo cáo do lỗi hệ thống")
    })
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
    @Operation(summary = "Chi tiết báo cáo QC", description = "Lấy thông tin báo cáo QC theo ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trả về báo cáo QC"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy báo cáo QC")
    })
    public ResponseEntity<?> getReport(@PathVariable Long reportId) {
        var response = qcReportService.getReport(reportId);
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin báo cáo QC thành công!",
                "Thông tin chi tiết báo cáo QC",
                response,
                HttpStatus.OK
        );
    }

    @PutMapping(value = "/pre-rental/{reportId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Cập nhật báo cáo QC PRE-RENTAL", description = "Payload tương tự tạo mới PRE_RENTAL")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật báo cáo QC thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy báo cáo QC")
    })
    public ResponseEntity<?> updatePreRentalReport(@PathVariable Long reportId,
                                                   @RequestPart("request") @Valid QCReportPreRentalUpdateRequestDto request,
                                                   @RequestPart(value = "accessorySnapshot", required = false) MultipartFile accessorySnapshot,
                                                   Authentication authentication) {
        String username = authentication.getName();
        var response = qcReportService.updatePreRentalReport(reportId, request, accessorySnapshot, username);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật báo cáo QC PRE_RENTAL thành công!",
                "Báo cáo QC đã được cập nhật",
                response,
                HttpStatus.OK
        );
    }

    @PutMapping(value = "/post-rental/{reportId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Cập nhật báo cáo QC POST-RENTAL", description = "Payload tương tự tạo mới POST_RENTAL")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật báo cáo QC thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy báo cáo QC")
    })
    public ResponseEntity<?> updatePostRentalReport(@PathVariable Long reportId,
                                                    @RequestPart("request") @Valid QCReportPostRentalUpdateRequestDto request,
                                                    @RequestPart(value = "accessorySnapshot", required = false) MultipartFile accessorySnapshot,
                                                    Authentication authentication) {
        String username = authentication.getName();
        var response = qcReportService.updatePostRentalReport(reportId, request, accessorySnapshot, username);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật báo cáo QC POST_RENTAL thành công!",
                "Báo cáo QC đã được cập nhật",
                response,
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Danh sách báo cáo QC theo đơn hàng", description = "Lấy báo cáo QC gắn với một đơn hàng")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trả về danh sách báo cáo QC"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng hoặc báo cáo")
    })
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
