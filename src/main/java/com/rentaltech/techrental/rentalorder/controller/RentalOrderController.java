package com.rentaltech.techrental.rentalorder.controller;

import com.rentaltech.techrental.common.util.PageableUtil;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rental-orders")
@Tag(name = "Quản lý đơn thuê", description = "Các API tạo, xem, cập nhật, gia hạn và xóa đơn thuê thiết bị")
public class RentalOrderController {

    private final RentalOrderService service;
    private final RentalOrderNotificationScheduler notificationScheduler;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tạo đơn thuê", description = "Khởi tạo đơn thuê thiết bị với đầy đủ thông tin")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo đơn thuê thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể tạo đơn thuê do lỗi hệ thống")
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
    @Operation(summary = "Chi tiết đơn thuê", description = "Lấy thông tin đơn thuê theo mã, bao gồm đầy đủ chi tiết")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin đơn thuê"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn thuê"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Mã đơn thuê") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Đơn thuê tìm thấy",
                "Đơn thuê với mã " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Danh sách đơn thuê", description = "Lấy danh sách tất cả đơn thuê cùng thông tin chi tiết")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách đơn thuê"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
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
    @Operation(summary = "Kích hoạt thông báo đơn sắp đến hạn", description = "Chạy ngay tác vụ notifyNearDueOrders để gửi thông báo đơn IN_USE còn dưới 2 ngày")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã kích hoạt tác vụ thông báo"),
            @ApiResponse(responseCode = "500", description = "Không thể kích hoạt tác vụ do lỗi hệ thống")
    })
    public ResponseEntity<?> triggerNearDueNotification() {
        notificationScheduler.notifyNearDueOrders();
        return ResponseUtil.createSuccessResponse(
                "Đã chạy tác vụ thông báo",
                "Đã kích hoạt quét các đơn IN_USE sắp đến hạn",
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('CUSTOMER')")
    @Operation(summary = "Cập nhật đơn thuê", description = "Chỉnh sửa thông tin đơn thuê theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật đơn thuê thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn thuê"),
            @ApiResponse(responseCode = "500", description = "Không thể cập nhật do lỗi hệ thống")
    })
    public ResponseEntity<?> update(@Parameter(description = "Mã đơn thuê") @PathVariable Long id,
                                    @Valid @RequestBody RentalOrderRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Đơn thuê được cập nhật thành công",
                "Đơn thuê với mã " + id + " đã cập nhật vào hệ thống",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{id}/confirm-return")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách xác nhận trả đơn", description = "Khách xác nhận sẽ trả hàng khi hết hạn thuê, hệ thống tạo task thu hồi đơn")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác nhận trả đơn thành công"),
            @ApiResponse(responseCode = "400", description = "Trạng thái không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn thuê"),
            @ApiResponse(responseCode = "500", description = "Không thể xử lý do lỗi hệ thống")
    })
    public ResponseEntity<?> confirmReturn(@Parameter(description = "Mã đơn thuê") @PathVariable Long id) {
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Gia hạn đơn thuê thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu gia hạn không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể gia hạn do lỗi hệ thống")
    })
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
    @Operation(summary = "Xóa đơn thuê", description = "Xóa đơn thuê theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa đơn thuê thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn thuê"),
            @ApiResponse(responseCode = "500", description = "Không thể xóa do lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Mã đơn thuê") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Đơn thuê được xóa thành công",
                "Đơn thuê với mã " + id + " đã xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Tìm kiếm/lọc đơn thuê", description = "Tra cứu đơn thuê với phân trang, sắp xếp và lọc theo nhiều tiêu chí")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách đơn thuê theo điều kiện"),
            @ApiResponse(responseCode = "500", description = "Không thể tìm kiếm do lỗi hệ thống")
    })
    public ResponseEntity<?> search(
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String startDateFrom,
            @RequestParam(required = false) String startDateTo,
            @RequestParam(required = false) String endDateFrom,
            @RequestParam(required = false) String endDateTo,
            @RequestParam(required = false) String createdAtFrom,
            @RequestParam(required = false) String createdAtTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        Pageable pageable = PageableUtil.buildPageRequest(page, size, sort);
        var pageResult = service.search(orderStatus, customerId, startDateFrom, startDateTo, endDateFrom, endDateTo, createdAtFrom, createdAtTo, pageable);
        return ResponseUtil.createSuccessPaginationResponse(
                "Kết quả tìm kiếm đơn thuê",
                "Áp dụng phân trang/sắp xếp/lọc theo tham số",
                pageResult,
                HttpStatus.OK
        );
    }
}
