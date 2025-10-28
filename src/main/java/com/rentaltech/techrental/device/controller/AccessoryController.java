package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.AccessoryRequestDto;
import com.rentaltech.techrental.device.service.AccessoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/accessories")
@Tag(name = "Accessories", description = "Accessory management APIs")
public class AccessoryController {

    private final AccessoryService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create accessory", description = "Create a new accessory")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> create(@Valid @RequestBody AccessoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện được tạo thành công",
                "Phụ kiện đã được thêm vào hệ thống",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get accessory by ID", description = "Retrieve accessory by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Accessory ID") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện tìm thấy",
                "Phụ kiện với id " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "List accessories", description = "Retrieve all accessories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tất cả phụ kiện",
                "Danh sách tất cả phụ kiện trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update accessory", description = "Update accessory by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> update(@Parameter(description = "Accessory ID") @PathVariable Long id,
                                    @Valid @RequestBody AccessoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện được cập nhật thành công",
                "Phụ kiện với id " + id + " đã cập nhật vào hệ thống",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete accessory", description = "Delete accessory by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Accessory ID") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện được xóa thành công",
                "Phụ kiện với id " + id + " đã xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }
}

