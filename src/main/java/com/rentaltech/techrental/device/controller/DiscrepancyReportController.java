package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportRequestDto;
import com.rentaltech.techrental.device.service.DiscrepancyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discrepancies")
@RequiredArgsConstructor
@Tag(name = "Báo cáo sai khác thiết bị", description = "Quản lý báo cáo sai khác thiết bị")
public class DiscrepancyReportController {

    private final DiscrepancyReportService discrepancyReportService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Tạo báo cáo sai khác", description = "Ghi nhận báo cáo sai khác cho thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo báo cáo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> create(@Valid @RequestBody DiscrepancyReportRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Tạo báo cáo sai khác thành công",
                "Báo cáo sai khác mới đã được tạo",
                discrepancyReportService.create(request),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Cập nhật báo cáo sai khác", description = "Điều chỉnh nội dung báo cáo sai khác hiện có")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật báo cáo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy báo cáo"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody DiscrepancyReportRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật báo cáo sai khác thành công",
                "Báo cáo sai khác đã được cập nhật",
                discrepancyReportService.update(id, request),
                HttpStatus.OK
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết báo cáo sai khác", description = "Xem thông tin báo cáo sai khác theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin báo cáo"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy báo cáo"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Chi tiết báo cáo sai khác",
                "Báo cáo sai khác theo mã",
                discrepancyReportService.getById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách báo cáo sai khác", description = "Có thể lọc theo nguồn tạo và mã tham chiếu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách báo cáo sai khác"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> list(@RequestParam(required = false) DiscrepancyCreatedFrom createdFrom,
                                  @RequestParam(required = false) Long refId) {
        var data = (createdFrom != null && refId != null)
                ? discrepancyReportService.getByReference(createdFrom, refId)
                : discrepancyReportService.getAll();
        return ResponseUtil.createSuccessResponse(
                "Danh sách báo cáo sai khác",
                "Toàn bộ báo cáo sai khác phù hợp",
                data,
                HttpStatus.OK
        );
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Danh sách sai khác theo đơn thuê", description = "Liệt kê toàn bộ discrepancy của order")
    public ResponseEntity<?> listByOrder(@PathVariable Long orderId) {
        return ResponseUtil.createSuccessResponse(
                "Danh sách báo cáo sai khác theo đơn",
                "Các báo cáo sai khác gắn với order " + orderId,
                discrepancyReportService.getByOrderId(orderId),
                HttpStatus.OK
        );
    }
}
