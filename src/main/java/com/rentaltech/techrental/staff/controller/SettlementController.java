package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.Settlement;
import com.rentaltech.techrental.staff.model.dto.SettlementCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.SettlementResponseDto;
import com.rentaltech.techrental.staff.model.dto.SettlementUpdateRequestDto;
import com.rentaltech.techrental.staff.service.settlementservice.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Tag(name = "Quản lý settlement", description = "Các API tạo, cập nhật và phản hồi settlement sau thuê")
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Tạo settlement", description = "Tạo một settlement mới ở trạng thái Draft")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo settlement thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể tạo settlement do lỗi hệ thống")
    })
    public ResponseEntity<?> create(@Valid @RequestBody SettlementCreateRequestDto request) {
        Settlement settlement = settlementService.create(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo settlement thành công",
                "Settlement đã được tạo ở trạng thái Draft",
                SettlementResponseDto.from(settlement),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Cập nhật settlement", description = "Chỉnh sửa thông tin settlement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật settlement thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy settlement"),
            @ApiResponse(responseCode = "500", description = "Không thể cập nhật do lỗi hệ thống")
    })
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody SettlementUpdateRequestDto request) {
        Settlement settlement = settlementService.update(id, request);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật settlement thành công",
                "Settlement đã được cập nhật",
                SettlementResponseDto.from(settlement),
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Settlement theo order", description = "Lấy settlement dựa trên order ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về settlement theo order ID"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy settlement"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getByOrderId(@PathVariable Long orderId) {
        Settlement settlement = settlementService.getByOrderId(orderId);
        return ResponseUtil.createSuccessResponse(
                "Lấy settlement thành công",
                "Settlement theo order ID",
                SettlementResponseDto.from(settlement),
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Danh sách settlement", description = "Trả về tất cả settlement kèm phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách settlement"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getAll(Pageable pageable) {
        var page = settlementService.getAll(pageable);
        var pageDto = page.map(SettlementResponseDto::from);
        return ResponseUtil.createSuccessPaginationResponse(
                "Danh sách tất cả settlement",
                "Tất cả settlements trong hệ thống với phân trang",
                pageDto,
                HttpStatus.OK
        );
    }

    @PatchMapping("/{id}/respond")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách phản hồi settlement", description = "Khách xác nhận hoặc từ chối settlement; xác nhận sẽ chuyển trạng thái sang Issued")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xử lý phản hồi settlement thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy settlement"),
            @ApiResponse(responseCode = "500", description = "Không thể phản hồi do lỗi hệ thống")
    })
    public ResponseEntity<?> respond(@PathVariable Long id,
                                     @RequestParam boolean accepted,
                                     org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Settlement settlement = settlementService.respondToSettlement(id, accepted, username);
        String title = accepted ? "Xác nhận settlement thành công" : "Từ chối settlement";
        String message = accepted ? "Settlement đã chuyển sang trạng thái Issued" : "Settlement được chuyển về Draft";
        return ResponseUtil.createSuccessResponse(
                title,
                message,
                SettlementResponseDto.from(settlement),
                HttpStatus.OK
        );
    }

}

