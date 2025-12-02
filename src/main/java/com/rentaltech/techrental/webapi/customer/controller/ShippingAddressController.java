package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.customer.model.dto.ShippingAddressRequestDto;
import com.rentaltech.techrental.webapi.customer.service.ShippingAddressService;
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
@RequiredArgsConstructor
@RequestMapping("/api/shipping-addresses")
@Tag(name = "Địa chỉ giao hàng", description = "API quản lý địa chỉ giao hàng dành cho khách hàng")
public class ShippingAddressController {

    private final ShippingAddressService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tạo địa chỉ giao hàng", description = "Khách hàng thêm mới địa chỉ giao hàng của mình")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo địa chỉ giao hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu địa chỉ không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể xử lý do lỗi hệ thống")
    })
    public ResponseEntity<?> create(@Valid @RequestBody ShippingAddressRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Thêm địa chỉ giao hàng thành công",
                "Địa chỉ giao hàng mới đã được lưu lại",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Chi tiết địa chỉ giao hàng", description = "Tra cứu địa chỉ giao hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về địa chỉ giao hàng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy địa chỉ"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Tìm thấy địa chỉ giao hàng",
                "Địa chỉ giao hàng có ID " + id,
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Danh sách địa chỉ giao hàng", description = "Trả về tất cả địa chỉ; khách chỉ thấy địa chỉ của mình")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách địa chỉ giao hàng"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách địa chỉ giao hàng",
                "Tất cả địa chỉ đáp ứng điều kiện truy vấn",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cập nhật địa chỉ giao hàng", description = "Chỉnh sửa địa chỉ giao hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật địa chỉ giao hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy địa chỉ giao hàng"),
            @ApiResponse(responseCode = "500", description = "Không thể cập nhật do lỗi hệ thống")
    })
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ShippingAddressRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật địa chỉ giao hàng thành công",
                "Địa chỉ giao hàng đã được cập nhật",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Xóa địa chỉ giao hàng", description = "Xóa địa chỉ giao hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa địa chỉ giao hàng thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy địa chỉ giao hàng"),
            @ApiResponse(responseCode = "500", description = "Không thể xóa do lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Xóa địa chỉ giao hàng thành công",
                "Địa chỉ giao hàng đã được xóa",
                HttpStatus.NO_CONTENT
        );
    }
}

