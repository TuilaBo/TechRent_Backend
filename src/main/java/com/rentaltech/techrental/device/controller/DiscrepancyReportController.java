package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportRequestDto;
import com.rentaltech.techrental.device.service.DiscrepancyReportService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Discrepancy Reports", description = "Quản lý báo cáo sai khác thiết bị")
public class DiscrepancyReportController {

    private final DiscrepancyReportService discrepancyReportService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Tạo discrepancy report")
    public ResponseEntity<?> create(@Valid @RequestBody DiscrepancyReportRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Tạo báo cáo sai khác thành công",
                "Discrepancy report mới đã được tạo",
                discrepancyReportService.create(request),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Cập nhật discrepancy report")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody DiscrepancyReportRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật báo cáo sai khác thành công",
                "Discrepancy report đã được cập nhật",
                discrepancyReportService.update(id, request),
                HttpStatus.OK
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết discrepancy report")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Chi tiết báo cáo sai khác",
                "Discrepancy report theo ID",
                discrepancyReportService.getById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách discrepancy report", description = "Có thể lọc theo nguồn tạo và refId")
    public ResponseEntity<?> list(@RequestParam(required = false) DiscrepancyCreatedFrom createdFrom,
                                  @RequestParam(required = false) Long refId) {
        var data = (createdFrom != null && refId != null)
                ? discrepancyReportService.getByReference(createdFrom, refId)
                : discrepancyReportService.getAll();
        return ResponseUtil.createSuccessResponse(
                "Danh sách báo cáo sai khác",
                "Toàn bộ discrepancy report phù hợp",
                data,
                HttpStatus.OK
        );
    }
}
