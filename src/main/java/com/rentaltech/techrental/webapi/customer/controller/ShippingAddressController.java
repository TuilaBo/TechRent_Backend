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
@Tag(name = "Shipping Address", description = "Shipping address management APIs")
public class ShippingAddressController {

    private final ShippingAddressService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create shipping address", description = "Create a new shipping address for a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> create(@Valid @RequestBody ShippingAddressRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Thêm địa chỉ giao hàng thành công",
                "Shipping address has been created",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Get shipping address by ID", description = "Retrieve shipping address by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Tìm thấy địa chỉ giao hàng",
                "Shipping address found with id " + id,
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "List shipping addresses", description = "Retrieve all shipping addresses; customers only see their own")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách địa chỉ giao hàng",
                "All shipping addresses",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update shipping address", description = "Update shipping address by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ShippingAddressRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật địa chỉ giao hàng thành công",
                "Shipping address updated",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete shipping address", description = "Delete shipping address by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Xóa địa chỉ giao hàng thành công",
                "Shipping address deleted",
                HttpStatus.NO_CONTENT
        );
    }
}

