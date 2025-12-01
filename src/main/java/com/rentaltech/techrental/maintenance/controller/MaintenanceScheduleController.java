package com.rentaltech.techrental.maintenance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.maintenance.model.CalendarScope;
import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.model.MaintenanceScheduleStatus;
import com.rentaltech.techrental.maintenance.model.dto.*;
import com.rentaltech.techrental.maintenance.service.MaintenanceScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance/schedules")
@RequiredArgsConstructor
@Tag(name = "Lịch bảo trì", description = "Quản lý lịch bảo trì thiết bị")
public class MaintenanceScheduleController {

    private final MaintenanceScheduleService maintenanceScheduleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Tạo lịch bảo trì", description = "Tạo lịch bảo trì cho thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo lịch bảo trì thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể tạo lịch do lỗi hệ thống")
    })
    public ResponseEntity<?> create(@RequestBody @Valid CreateScheduleRequestDto request) {
        MaintenanceSchedule data = maintenanceScheduleService.createSchedule(
                request.getDeviceId(),
                null,
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus()
        );
        return ResponseUtil.createSuccessResponse("Tạo lịch bảo trì thành công", "Lịch bảo trì đã được tạo", data, HttpStatus.CREATED);
    }

    @PostMapping("/by-category")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Tạo lịch theo danh mục thiết bị", description = "Sinh lịch bảo trì cho toàn bộ thiết bị thuộc category được chọn")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo lịch theo danh mục thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu danh mục không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể tạo lịch do lỗi hệ thống")
    })
    public ResponseEntity<?> createByCategory(@RequestBody @Valid MaintenanceScheduleByCategoryRequestDto request) {
        List<MaintenanceSchedule> data = maintenanceScheduleService.createSchedulesByCategory(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo lịch bảo trì theo category thành công",
                "Đã tạo " + data.size() + " lịch bảo trì",
                data,
                HttpStatus.CREATED
        );
    }

    @PostMapping("/by-usage")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Tạo lịch theo số lần sử dụng", description = "Sinh lịch bảo trì dựa trên ngưỡng số lần sử dụng của thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo lịch theo số lần sử dụng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu số lần sử dụng không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể tạo lịch do lỗi hệ thống")
    })
    public ResponseEntity<?> createByUsage(@RequestBody @Valid MaintenanceScheduleByUsageRequestDto request) {
        List<MaintenanceSchedule> data = maintenanceScheduleService.createSchedulesByUsage(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo lịch bảo trì theo số lần sử dụng thành công",
                "Đã tạo " + data.size() + " lịch bảo trì",
                data,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/priority")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Danh sách thiết bị cần ưu tiên bảo trì", description = "Trả về các thiết bị có nhu cầu bảo trì gấp")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thiết bị ưu tiên"),
            @ApiResponse(responseCode = "500", description = "Không thể lấy danh sách do lỗi hệ thống")
    })
    public ResponseEntity<?> getPriorityMaintenanceDevices() {
        List<PriorityMaintenanceDeviceDto> devices = maintenanceScheduleService.getPriorityMaintenanceDevices();
        return ResponseUtil.createSuccessResponse(
                "Danh sách thiết bị cần ưu tiên bảo trì",
                "Có " + devices.size() + " thiết bị cần bảo trì",
                devices,
                HttpStatus.OK
        );
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Danh sách lịch bảo trì đang hoạt động", description = "Trả về các lịch bảo trì còn hiệu lực")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách lịch đang hoạt động"),
            @ApiResponse(responseCode = "500", description = "Không thể lấy danh sách do lỗi hệ thống")
    })
    public ResponseEntity<?> getActiveMaintenanceSchedules() {
        List<MaintenanceSchedule> data = maintenanceScheduleService.getActiveMaintenanceSchedules();
        return ResponseUtil.createSuccessResponse(
                "Danh sách thiết bị đang bảo trì",
                "Có " + data.size() + " thiết bị đang bảo trì",
                data,
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Danh sách lịch bảo trì theo thiết bị", description = "Lấy lịch bảo trì của một thiết bị cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách lịch bảo trì"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị hoặc thiếu tham số deviceId"),
            @ApiResponse(responseCode = "500", description = "Không thể lấy lịch do lỗi hệ thống")
    })
    public ResponseEntity<?> listByDevice(@RequestParam("deviceId") Long deviceId) {
        List<MaintenanceSchedule> data = maintenanceScheduleService.listByDevice(deviceId);
        return ResponseUtil.createSuccessResponse("Danh sách lịch bảo trì", "Theo thiết bị", data, HttpStatus.OK);
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Danh sách lịch bảo trì theo ngày/tháng", description = "Tra cứu lịch bảo trì theo phạm vi ngày hoặc tháng, hỗ trợ phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách lịch theo phạm vi"),
            @ApiResponse(responseCode = "400", description = "Tham số thời gian không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể lấy danh sách do lỗi hệ thống")
    })
    public ResponseEntity<?> listByCalendarScope(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                 @RequestParam(value = "scope", defaultValue = "DAY") String scope,
                                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                                 @RequestParam(value = "size", defaultValue = "20") int size) {
        CalendarScope resolvedScope = CalendarScope.from(scope);
        LocalDate start = resolvedScope == CalendarScope.DAY ? date : date.withDayOfMonth(1);
        LocalDate end = resolvedScope == CalendarScope.DAY ? date : date.withDayOfMonth(date.lengthOfMonth());
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Page<MaintenanceSchedule> schedules = maintenanceScheduleService.listByDateRange(
                start,
                end,
                PageRequest.of(safePage, safeSize, Sort.by("startDate").ascending())
        );
        return ResponseUtil.createSuccessResponse(
                "Danh sách lịch bảo trì theo " + resolvedScope.name().toLowerCase(java.util.Locale.ROOT),
                String.format("Khoảng thời gian %s -> %s", start, end),
                schedules,
                HttpStatus.OK
        );
    }

    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Cập nhật trạng thái lịch bảo trì", description = "Thay đổi trạng thái của một lịch bảo trì")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu trạng thái không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lịch bảo trì"),
            @ApiResponse(responseCode = "500", description = "Không thể cập nhật do lỗi hệ thống")
    })
    public ResponseEntity<?> updateStatus(@PathVariable("id") Long id, @RequestBody @Valid UpdateStatusRequestDto request) {
        MaintenanceSchedule data = maintenanceScheduleService.updateStatus(id, request.getStatus(), request.getEvidenceUrls());
        return ResponseUtil.createSuccessResponse("Cập nhật trạng thái lịch bảo trì", "Trạng thái đã được cập nhật", data, HttpStatus.OK);
    }

    @PatchMapping(value = "/{id}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Cập nhật trạng thái kèm hình ảnh", description = "Cho phép tải trực tiếp ảnh bằng chứng từ thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái và hình ảnh thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu upload không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lịch bảo trì"),
            @ApiResponse(responseCode = "500", description = "Không thể cập nhật do lỗi hệ thống")
    })
    public ResponseEntity<?> updateStatusWithFiles(@PathVariable("id") Long id,
                                                   @RequestPart("status") MaintenanceScheduleStatus status,
                                                   @RequestPart(value = "evidenceUrls", required = false) List<String> evidenceUrls,
                                                   @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        MaintenanceSchedule data = maintenanceScheduleService.updateStatusWithUploads(id, status, evidenceUrls, files);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật trạng thái lịch bảo trì",
                "Trạng thái và bằng chứng đã được cập nhật",
                data,
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Xóa lịch bảo trì", description = "Xóa một lịch bảo trì cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa lịch bảo trì thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lịch bảo trì cần xóa"),
            @ApiResponse(responseCode = "500", description = "Không thể xóa do lỗi hệ thống")
    })
    public ResponseEntity<?> deleteSchedule(@PathVariable("id") Long id) {
        maintenanceScheduleService.deleteSchedule(id);
        return ResponseUtil.createSuccessResponse(
                "Xóa lịch bảo trì thành công",
                "Lịch bảo trì đã được xóa",
                HttpStatus.OK
        );
    }
}


