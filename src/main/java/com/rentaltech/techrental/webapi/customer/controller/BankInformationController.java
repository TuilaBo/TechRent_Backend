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
@Tag(name = "Bank Information", description = "Bank information management APIs")
public class BankInformationController {

    private final BankInformationService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create bank information", description = "Create a new bank information for a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> create(@Valid @RequestBody BankInformationRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Thêm thông tin ngân hàng thành công",
                "Bank information has been created",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Get bank information by ID", description = "Retrieve bank information by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Tìm thấy thông tin ngân hàng",
                "Bank information found with id " + id,
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "List bank informations", description = "Retrieve all bank informations; customers only see their own")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách thông tin ngân hàng",
                "All bank informations",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update bank information", description = "Update bank information by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody BankInformationRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật thông tin ngân hàng thành công",
                "Bank information updated",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete bank information", description = "Delete bank information by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Xóa thông tin ngân hàng thành công",
                "Bank information deleted",
                HttpStatus.NO_CONTENT
        );
    }
}

