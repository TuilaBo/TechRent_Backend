package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.Dispute;
import com.rentaltech.techrental.staff.model.dto.DisputeCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.DisputeResponseDto;
import com.rentaltech.techrental.staff.model.dto.DisputeUpdateRequestDto;
import com.rentaltech.techrental.staff.service.disputeservice.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
@Tag(name = "Dispute", description = "Dispute management APIs")
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create dispute", description = "Customer creates a dispute when disagreeing with settlement")
    public ResponseEntity<?> create(@Valid @RequestBody DisputeCreateRequestDto request) {
        Dispute dispute = disputeService.create(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo dispute thành công",
                "Dispute đã được tạo ở trạng thái Open",
                mapToResponseDto(dispute),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Update dispute", description = "Update dispute details and status")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody DisputeUpdateRequestDto request) {
        Dispute dispute = disputeService.update(id, request);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật dispute thành công",
                "Dispute đã được cập nhật",
                mapToResponseDto(dispute),
                HttpStatus.OK
        );
    }

    @GetMapping("/settlement/{settlementId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('TECHNICIAN') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Get dispute by settlement ID", description = "Retrieve dispute by settlement ID")
    public ResponseEntity<?> getBySettlementId(@PathVariable Long settlementId) {
        Dispute dispute = disputeService.getBySettlementId(settlementId);
        return ResponseUtil.createSuccessResponse(
                "Lấy dispute thành công",
                "Dispute theo settlement ID",
                mapToResponseDto(dispute),
                HttpStatus.OK
        );
    }

    private DisputeResponseDto mapToResponseDto(Dispute dispute) {
        return DisputeResponseDto.builder()
                .disputeId(dispute.getDisputeId())
                .settlementId(dispute.getSettlement() != null ? dispute.getSettlement().getSettlementId() : null)
                .openBy(dispute.getOpenBy())
                .openedByCustomerId(dispute.getOpenedByCustomer() != null ? dispute.getOpenedByCustomer().getCustomerId() : null)
                .openedByStaffId(dispute.getOpenedByStaff() != null ? dispute.getOpenedByStaff().getStaffId() : null)
                .reason(dispute.getReason())
                .detail(dispute.getDetail())
                .status(dispute.getStatus())
                .openAt(dispute.getOpenAt())
                .closedAt(dispute.getClosedAt())
                .build();
    }
}

