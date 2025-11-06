package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.DeviceRequestDto;
import com.rentaltech.techrental.device.service.DeviceService;
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
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
@Tag(name = "Devices", description = "Device management APIs")
public class DeviceController {

    private final DeviceService service;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create device", description = "Create a new device")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "500", description = "Server error")
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
    @Operation(summary = "Get device by ID", description = "Retrieve device by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Device ID") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Thiết bị tìm thấy",
                "Thiết bị với id " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "List devices", description = "Retrieve all devices")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
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
    @Operation(summary = "Get devices by model", description = "Retrieve all devices belonging to a model id")
    public ResponseEntity<?> getByModel(@PathVariable Long deviceModelId) {
        return ResponseUtil.createSuccessResponse(
                "Danh sách thiết bị theo model",
                "Danh sách thiết bị của model " + deviceModelId,
                service.findByModelId(deviceModelId),
                HttpStatus.OK
        );
    }

    @GetMapping("/order-detail/{orderDetailId}")
    @Operation(summary = "Get devices by order detail", description = "Retrieve allocated devices for an order detail")
    public ResponseEntity<?> getByOrderDetail(@PathVariable Long orderDetailId,
                                              Authentication authentication) {
        OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy OrderDetail với id: " + orderDetailId));

        if (authentication != null && hasRole(authentication, "ROLE_CUSTOMER")) {
            Long ownerCustomerId = orderDetail.getRentalOrder().getCustomer().getCustomerId();
            Long currentCustomerId = customerService.getCustomerByUsernameOrThrow(authentication.getName()).getCustomerId();
            if (!ownerCustomerId.equals(currentCustomerId)) {
                return ResponseUtil.createErrorResponse(
                        "FORBIDDEN",
                        "Không có quyền truy cập",
                        "Order detail không thuộc quyền sở hữu",
                        HttpStatus.FORBIDDEN
                );
            }
        }

        return ResponseUtil.createSuccessResponse(
                "Danh sách thiết bị theo order detail",
                "Danh sách thiết bị của order detail " + orderDetailId,
                service.findByOrderDetail(orderDetailId),
                HttpStatus.OK
        );
    }

    @GetMapping("/serial/{serialNumber}")
    @Operation(summary = "Get device by serial number", description = "Retrieve device by serial number")
    public ResponseEntity<?> getBySerial(@PathVariable String serialNumber) {
        return ResponseUtil.createSuccessResponse(
                "Thiết bị theo serial",
                "Thiết bị với serial " + serialNumber,
                service.findBySerialNumber(serialNumber),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update device", description = "Update device by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> update(@Parameter(description = "Device ID") @PathVariable Long id,
                                    @Valid @RequestBody DeviceRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Thiết bị được cập nhật thành công",
                "Thiết bị với id " + id + " đã được cập nhật",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete device", description = "Delete device by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "404", description = "Not found"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Device ID") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Thiết bị được xóa thành công",
                "Thiết bị với id " + id + " đã bị xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Search/sort/filter devices", description = "Search devices with pagination, sorting and filtering")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
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
