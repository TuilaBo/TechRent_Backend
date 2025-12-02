package com.rentaltech.techrental.rentalorder.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.rentalorder.model.dto.RentalOrderExtendRequestDto;
import com.rentaltech.techrental.rentalorder.model.dto.RentalOrderRequestDto;
import com.rentaltech.techrental.rentalorder.service.RentalOrderService;
import com.rentaltech.techrental.config.RentalOrderNotificationScheduler;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rental-orders")
@Tag(name = "Rental Orders", description = "Rental order management APIs")
public class RentalOrderController {

    private final RentalOrderService service;
    private final RentalOrderNotificationScheduler notificationScheduler;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create rental order", description = "Create a new rental order with details")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> create(@Valid @RequestBody RentalOrderRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Đơn thuê được tạo thành công",
                "Đơn thuê đã được thêm vào hệ thống",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Get rental order by ID", description = "Retrieve rental order by ID with details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Rental order ID") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Đơn thuê tìm thấy",
                "Đơn thuê với id " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "List rental orders", description = "Retrieve all rental orders with details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tất cả đơn thuê",
                "Danh sách tất cả đơn thuê trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }
    
    @PostMapping("/notify-near-due")
//    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Kích hoạt thông báo đơn sắp đến hạn", description = "Chạy ngay job notifyNearDueOrders để gửi thông báo đơn IN_USE còn dưới 2 ngày")
    public ResponseEntity<?> triggerNearDueNotification() {
        notificationScheduler.notifyNearDueOrders();
        return ResponseUtil.createSuccessResponse(
                "Đã chạy job thông báo",
                "Đã kích hoạt scan đơn IN_USE sắp đến hạn",
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('CUSTOMER')")
    @Operation(summary = "Update rental order", description = "Update rental order by ID; change order details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> update(@Parameter(description = "Rental order ID") @PathVariable Long id,
                                    @Valid @RequestBody RentalOrderRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Đơn thuê được cập nhật thành công",
                "Đơn thuê với id " + id + " đã cập nhật vào hệ thống",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{id}/confirm-return")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách xác nhận trả đơn", description = "Khách xác nhận sẽ trả hàng khi hết hạn thuê, hệ thống tạo task thu hồi đơn")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Confirmed"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> confirmReturn(@Parameter(description = "Rental order ID") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Xác nhận trả đơn thành công",
                "Đã tạo nhiệm vụ thu hồi cho đơn thuê",
                service.confirmReturn(id),
                HttpStatus.OK
        );
    }

    @PostMapping("/extend")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Gia hạn đơn thuê", description = "Tạo đơn gia hạn từ đơn thuê hiện có")
    public ResponseEntity<?> extend(@Valid @RequestBody RentalOrderExtendRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Gia hạn đơn thuê thành công",
                "Đã tạo đơn gia hạn mới",
                service.extend(request),
                HttpStatus.CREATED
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Delete rental order", description = "Delete rental order by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Rental order ID") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Đơn thuê được xóa thành công",
                "Đơn thuê với id " + id + " đã xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Search/sort/filter rental orders", description = "Search rental orders with pagination, sorting and filtering using Specification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> search(
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String shippingAddress,
            @RequestParam(required = false) BigDecimal minTotalPrice,
            @RequestParam(required = false) BigDecimal maxTotalPrice,
            @RequestParam(required = false) BigDecimal minPricePerDay,
            @RequestParam(required = false) BigDecimal maxPricePerDay,
            @RequestParam(required = false) String startDateFrom,
            @RequestParam(required = false) String startDateTo,
            @RequestParam(required = false) String endDateFrom,
            @RequestParam(required = false) String endDateTo,
            @RequestParam(required = false) String createdAtFrom,
            @RequestParam(required = false) String createdAtTo, Pageable pageable) {
        var page = service.search(orderStatus, customerId, shippingAddress, minTotalPrice, maxTotalPrice, minPricePerDay, maxPricePerDay, startDateFrom, startDateTo, endDateFrom, endDateTo, createdAtFrom, createdAtTo, pageable);
        return ResponseUtil.createSuccessPaginationResponse(
                "Kết quả tìm kiếm đơn thuê",
                "Áp dụng phân trang/sắp xếp/lọc theo tham số",
                page,
                HttpStatus.OK
        );
    }
}

