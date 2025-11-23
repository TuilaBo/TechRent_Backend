package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.Settlement;
import com.rentaltech.techrental.staff.model.dto.SettlementCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.SettlementResponseDto;
import com.rentaltech.techrental.staff.model.dto.SettlementUpdateRequestDto;
import com.rentaltech.techrental.staff.service.settlementservice.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Settlement", description = "Settlement management APIs")
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Create settlement", description = "Create a new settlement in Draft state")
    public ResponseEntity<?> create(@Valid @RequestBody SettlementCreateRequestDto request) {
        Settlement settlement = settlementService.create(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo settlement thành công",
                "Settlement đã được tạo ở trạng thái Draft",
                mapToResponseDto(settlement),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Update settlement", description = "Update settlement details")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody SettlementUpdateRequestDto request) {
        Settlement settlement = settlementService.update(id, request);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật settlement thành công",
                "Settlement đã được cập nhật",
                mapToResponseDto(settlement),
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Get settlement by order ID", description = "Retrieve settlement by order ID")
    public ResponseEntity<?> getByOrderId(@PathVariable Long orderId) {
        Settlement settlement = settlementService.getByOrderId(orderId);
        return ResponseUtil.createSuccessResponse(
                "Lấy settlement thành công",
                "Settlement theo order ID",
                mapToResponseDto(settlement),
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Get all settlements", description = "Retrieve all settlements with pagination")
    public ResponseEntity<?> getAll(Pageable pageable) {
        var page = settlementService.getAll(pageable);
        var pageDto = page.map(this::mapToResponseDto);
        return ResponseUtil.createSuccessPaginationResponse(
                "Danh sách tất cả settlement",
                "Tất cả settlements trong hệ thống với phân trang",
                pageDto,
                HttpStatus.OK
        );
    }

    @PatchMapping("/{id}/respond")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách xác nhận/ từ chối settlement", description = "Khách phản hồi settlement, nếu xác nhận sẽ chuyển state sang Issued")
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
                mapToResponseDto(settlement),
                HttpStatus.OK
        );
    }

    private SettlementResponseDto mapToResponseDto(Settlement settlement) {
        return SettlementResponseDto.builder()
                .settlementId(settlement.getSettlementId())
                .orderId(settlement.getRentalOrder() != null ? settlement.getRentalOrder().getOrderId() : null)
                .totalDeposit(settlement.getTotalDeposit())
                .damageFee(settlement.getDamageFee())
                .lateFee(settlement.getLateFee())
                .accessoryFee(settlement.getAccessoryFee())
                .finalReturnAmount(settlement.getFinalReturnAmount())
                .state(settlement.getState())
                .issuedAt(settlement.getIssuedAt())
                .build();
    }
}

