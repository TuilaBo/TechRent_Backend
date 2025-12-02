package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.customer.model.dto.BankInformationRequestDto;
import com.rentaltech.techrental.webapi.customer.service.BankInformationService;
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
@RequestMapping("/api/bank-informations")
@Tag(name = "Quản lý tài khoản ngân hàng", description = "API cho khách hàng thêm và quản lý thông tin ngân hàng nhận tiền")
public class BankInformationController {

    private final BankInformationService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Thêm thông tin ngân hàng", description = "Khách hàng đăng ký tài khoản ngân hàng mới để nhận tiền")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo thông tin ngân hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu thông tin ngân hàng không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể xử lý do lỗi hệ thống")
    })
    public ResponseEntity<?> create(@Valid @RequestBody BankInformationRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Thêm thông tin ngân hàng thành công",
                "Thông tin ngân hàng đã được tạo",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Chi tiết thông tin ngân hàng", description = "Lấy thông tin ngân hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin ngân hàng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông tin ngân hàng"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Tìm thấy thông tin ngân hàng",
                "Thông tin ngân hàng với id " + id,
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Danh sách thông tin ngân hàng", description = "Lấy toàn bộ thông tin ngân hàng (khách hàng chỉ thấy của mình)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thông tin ngân hàng"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách thông tin ngân hàng",
                "Danh sách thông tin ngân hàng",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cập nhật thông tin ngân hàng", description = "Cập nhật thông tin ngân hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thông tin ngân hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông tin ngân hàng"),
            @ApiResponse(responseCode = "500", description = "Không thể cập nhật do lỗi hệ thống")
    })
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody BankInformationRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật thông tin ngân hàng thành công",
                "Thông tin ngân hàng đã được cập nhật",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Xóa thông tin ngân hàng", description = "Xóa thông tin ngân hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa thông tin ngân hàng thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông tin ngân hàng"),
            @ApiResponse(responseCode = "500", description = "Không thể xóa do lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Xóa thông tin ngân hàng thành công",
                "Thông tin ngân hàng đã bị xóa",
                HttpStatus.NO_CONTENT
        );
    }
}

