package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.dto.DeviceReplacementReportResponseDto;
import com.rentaltech.techrental.staff.model.dto.DeviceReplacementReportStaffSignRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverPinDeliveryDto;
import com.rentaltech.techrental.staff.service.devicereplacement.DeviceReplacementReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/staff/device-replacement-reports")
@RequiredArgsConstructor
@Tag(name = "Biên bản đổi thiết bị", description = "API để quản lý biên bản đổi thiết bị khi có khiếu nại")
public class DeviceReplacementReportController {

    private final DeviceReplacementReportService replacementReportService;

    @GetMapping
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Danh sách biên bản đổi thiết bị", description = "Lấy toàn bộ biên bản đổi thiết bị hiện có")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách biên bản"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getAllReports() {
        List<DeviceReplacementReportResponseDto> reports = replacementReportService.getAllReports();
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách biên bản thành công",
                "Tất cả biên bản đổi thiết bị hiện có",
                reports,
                HttpStatus.OK
        );
    }

    @GetMapping("/{replacementReportId}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Chi tiết biên bản đổi thiết bị", description = "Xem chi tiết biên bản theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về chi tiết biên bản"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy biên bản"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getReport(@PathVariable Long replacementReportId) {
        DeviceReplacementReportResponseDto responseDto = replacementReportService.getReport(replacementReportId);
        return ResponseUtil.createSuccessResponse(
                "Lấy biên bản đổi thiết bị thành công",
                "Chi tiết biên bản đổi thiết bị",
                responseDto,
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Biên bản theo đơn hàng", description = "Liệt kê các biên bản đổi thiết bị gắn với một đơn hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách biên bản"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getReportsByOrder(@PathVariable Long orderId) {
        List<DeviceReplacementReportResponseDto> reports = replacementReportService.getReportsByOrder(orderId);
        return ResponseUtil.createSuccessResponse(
                "Lấy biên bản theo đơn hàng thành công",
                "Danh sách biên bản đổi thiết bị của đơn hàng",
                reports,
                HttpStatus.OK
        );
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Biên bản theo task", description = "Liệt kê biên bản được tạo từ một task cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách biên bản"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getReportsByTask(@PathVariable Long taskId) {
        List<DeviceReplacementReportResponseDto> reports = replacementReportService.getReportsByTask(taskId);
        return ResponseUtil.createSuccessResponse(
                "Lấy biên bản theo nhiệm vụ thành công",
                "Danh sách biên bản đổi thiết bị theo task",
                reports,
                HttpStatus.OK
        );
    }

    @PostMapping("/{replacementReportId}/pin")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Gửi PIN cho nhân viên", description = "Gửi lại mã PIN ký biên bản cho nhân viên phụ trách")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gửi PIN thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy biên bản"),
            @ApiResponse(responseCode = "500", description = "Không thể gửi PIN do lỗi hệ thống")
    })
    public ResponseEntity<?> requestPinForStaff(@PathVariable Long replacementReportId) {
        HandoverPinDeliveryDto responseDto = replacementReportService.sendPinToStaffForReport(replacementReportId);
        return ResponseUtil.createSuccessResponse(
                "Gửi PIN thành công",
                "Mã PIN đã được gửi đến email của bạn",
                responseDto,
                HttpStatus.OK
        );
    }

    @PatchMapping("/{replacementReportId}/signature")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Nhân viên ký biên bản", description = "Xác nhận biên bản đổi thiết bị bằng mã PIN của nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ký biên bản thành công"),
            @ApiResponse(responseCode = "400", description = "Mã PIN hoặc dữ liệu ký không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy biên bản"),
            @ApiResponse(responseCode = "500", description = "Không thể ký do lỗi hệ thống")
    })
    public ResponseEntity<?> signByStaff(
            @PathVariable Long replacementReportId,
            @Valid @RequestBody DeviceReplacementReportStaffSignRequestDto request) {
        DeviceReplacementReportResponseDto responseDto = replacementReportService.signByStaff(replacementReportId, request);
        return ResponseUtil.createSuccessResponse(
                "Ký biên bản thành công",
                "Bạn đã ký biên bản đổi thiết bị",
                responseDto,
                HttpStatus.OK
        );
    }

    @PostMapping(value = "/{replacementReportId}/devices/{deviceId}/evidences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Cập nhật bằng chứng thiết bị", description = "Tải lên danh sách ảnh cho thiết bị trong biên bản đổi thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật bằng chứng thành công"),
            @ApiResponse(responseCode = "400", description = "File tải lên không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy biên bản hoặc thiết bị"),
            @ApiResponse(responseCode = "500", description = "Không thể cập nhật do lỗi hệ thống")
    })
    public ResponseEntity<?> updateDeviceEvidence(
            @PathVariable Long replacementReportId,
            @PathVariable Long deviceId,
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails principal) {
        String username = principal.getUsername();
        com.rentaltech.techrental.staff.model.dto.DeviceReplacementReportItemResponseDto responseDto =
                replacementReportService.updateEvidenceByDevice(replacementReportId, deviceId, files, username);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật bằng chứng thiết bị thành công",
                "Danh sách hình ảnh cho thiết bị đã được cập nhật",
                responseDto,
                HttpStatus.OK
        );
    }
}

