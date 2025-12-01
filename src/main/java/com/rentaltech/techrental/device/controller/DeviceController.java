package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.DeviceConditionUpdateRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceRequestDto;
import com.rentaltech.techrental.device.service.DeviceService;
import com.rentaltech.techrental.device.service.DeviceConditionService;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.webapi.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
@Tag(name = "Quản lý thiết bị", description = "Các API phục vụ quản trị thiết bị, tình trạng, phân bổ và tra cứu chi tiết")
public class DeviceController {

    private final DeviceService service;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerService customerService;
    private final DeviceConditionService deviceConditionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo thiết bị", description = "Thêm mới một thiết bị vào hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo thiết bị thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> create(@Valid @RequestBody DeviceRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Thiết bị được tạo thành công",
                "Thiết bị đã được thêm vào hệ thống",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết thiết bị", description = "Lấy thông tin thiết bị theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin thiết bị"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Mã thiết bị") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Thiết bị tìm thấy",
                "Thiết bị với mã " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping("/{id}/conditions")
    @Operation(summary = "Danh sách tình trạng hiện tại của thiết bị", description = "Lấy lịch sử tình trạng gần nhất của thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách tình trạng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getDeviceConditions(@PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tình trạng thiết bị",
                "Tình trạng mới nhất của thiết bị",
                deviceConditionService.getByDevice(id),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}/conditions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('OPERATOR')")
    @Operation(summary = "Cập nhật tình trạng thiết bị", description = "Ghi nhận lại các hạng mục tình trạng mới nhất của thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật tình trạng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu tình trạng không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> upsertDeviceConditions(@PathVariable Long id,
                                                    @Valid @RequestBody DeviceConditionUpdateRequestDto request,
                                                    @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseUtil.unauthorized();
        }
        var updated = deviceConditionService.upsertConditions(id, request.getConditions(), principal.getUsername());
        return ResponseUtil.createSuccessResponse(
                "Cập nhật tình trạng thành công",
                "Tình trạng thiết bị đã được lưu",
                updated,
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách thiết bị", description = "Liệt kê toàn bộ thiết bị trong hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tất cả thiết bị",
                "Danh sách tất cả thiết bị trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @GetMapping("/model/{deviceModelId}")
    @Operation(summary = "Thiết bị khả dụng theo model", description = "Trả về danh sách thiết bị thuộc model còn trống trong khoảng thời gian yêu cầu (áp dụng buffer giống endpoint khả dụng)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thiết bị khả dụng"),
            @ApiResponse(responseCode = "400", description = "Khoảng thời gian không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getAvailableDevicesByModel(@PathVariable Long deviceModelId,
                                                        @RequestParam("start") LocalDateTime start,
                                                        @RequestParam("end") LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("start phải nhỏ hơn end");
        }
        LocalDateTime marginStart = start.minusDays(DeviceAvailabilityController.AVAILABILITY_MARGIN_DAYS);
        LocalDateTime marginEnd = end.plusDays(DeviceAvailabilityController.AVAILABILITY_MARGIN_DAYS);
        var devices = service.findAvailableByModelWithinRange(deviceModelId, marginStart, marginEnd);
        return ResponseUtil.createSuccessResponse(
                "Thiết bị khả dụng theo model",
                "Danh sách thiết bị model %d khả dụng trong khung thời gian yêu cầu (đã áp dụng buffer %d ngày)"
                        .formatted(deviceModelId, DeviceAvailabilityController.AVAILABILITY_MARGIN_DAYS),
                devices,
                HttpStatus.OK
        );
    }

    @GetMapping("/order-detail/{orderDetailId}")
    @Operation(summary = "Thiết bị theo chi tiết đơn thuê", description = "Lấy danh sách thiết bị đã được phân bổ cho một chi tiết đơn thuê")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thiết bị"),
            @ApiResponse(responseCode = "403", description = "Khách hàng không có quyền xem chi tiết đơn thuê"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy chi tiết đơn thuê"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getByOrderDetail(@PathVariable Long orderDetailId,
                                              Authentication authentication) {
        OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy chi tiết đơn thuê với mã: " + orderDetailId));

        if (hasRole(authentication, "ROLE_CUSTOMER")) {
            Long ownerCustomerId = orderDetail.getRentalOrder().getCustomer().getCustomerId();
            Long currentCustomerId = customerService.getCustomerByUsernameOrThrow(authentication.getName()).getCustomerId();
            if (!ownerCustomerId.equals(currentCustomerId)) {
                return ResponseUtil.createErrorResponse(
                        "FORBIDDEN",
                        "Không có quyền truy cập",
                        "Chi tiết đơn thuê không thuộc quyền sở hữu",
                        HttpStatus.FORBIDDEN
                );
            }
        }

        return ResponseUtil.createSuccessResponse(
                "Danh sách thiết bị theo chi tiết đơn thuê",
                "Danh sách thiết bị của chi tiết đơn thuê " + orderDetailId,
                service.findByOrderDetail(orderDetailId),
                HttpStatus.OK
        );
    }

    @GetMapping("/serial/{serialNumber}")
    @Operation(summary = "Tra thiết bị theo serial", description = "Lấy thông tin thiết bị dựa trên số serial")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin thiết bị"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị với serial cung cấp"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getBySerial(@PathVariable String serialNumber) {
        return ResponseUtil.createSuccessResponse(
                "Thiết bị theo serial",
                "Thiết bị với serial " + serialNumber,
                service.findBySerialNumber(serialNumber),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Cập nhật thiết bị", description = "Chỉnh sửa thông tin thiết bị theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thiết bị thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> update(@Parameter(description = "Mã thiết bị") @PathVariable Long id,
                                    @Valid @RequestBody DeviceRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Thiết bị được cập nhật thành công",
                "Thiết bị với mã " + id + " đã được cập nhật",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa thiết bị", description = "Xóa thiết bị khỏi hệ thống theo mã")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Xóa thiết bị thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy thiết bị"),
        @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Mã thiết bị") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Thiết bị được xóa thành công",
                "Thiết bị với mã " + id + " đã bị xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm/lọc thiết bị", description = "Tra cứu thiết bị với hỗ trợ phân trang, sắp xếp và lọc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thiết bị thỏa điều kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> search(
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) String shelfCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deviceModelId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String deviceName,
            Pageable pageable) {
        var page = service.search(serialNumber, shelfCode, status, deviceModelId, brand, deviceName, pageable);
        return ResponseUtil.createSuccessPaginationResponse(
                "Kết quả tìm kiếm thiết bị",
                "Áp dụng phân trang/sắp xếp/lọc theo tham số",
                page,
                HttpStatus.OK
        );
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
