/*package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.AccessoryCategoryRequestDto;
import com.rentaltech.techrental.device.service.AccessoryCategoryService;
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
@RequestMapping("/api/accessory-categories")
@Tag(name = "Quản lý danh mục phụ kiện", description = "Các API phục vụ tạo, xem, cập nhật và xóa danh mục phụ kiện thiết bị")
public class AccessoryCategoryController {

    private final AccessoryCategoryService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo danh mục phụ kiện", description = "Thêm mới một danh mục phụ kiện vào hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo danh mục phụ kiện thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu gửi lên không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống nội bộ")
    })
    public ResponseEntity<?> create(@Valid @RequestBody AccessoryCategoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Danh mục phụ kiện được tạo thành công",
                "Danh mục phụ kiện đã được thêm vào hệ thống",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết danh mục phụ kiện", description = "Lấy thông tin chi tiết danh mục phụ kiện theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin danh mục phụ kiện"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục phụ kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống nội bộ")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Mã danh mục phụ kiện") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Danh mục phụ kiện tìm thấy",
                "Danh mục phụ kiện với mã " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách danh mục phụ kiện", description = "Liệt kê toàn bộ danh mục phụ kiện hiện có")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách danh mục phụ kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống nội bộ")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tất cả danh mục phụ kiện",
                "Danh sách tất cả danh mục phụ kiện trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật danh mục phụ kiện", description = "Chỉnh sửa thông tin danh mục phụ kiện theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật danh mục phụ kiện thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục phụ kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống nội bộ")
    })
    public ResponseEntity<?> update(@Parameter(description = "Mã danh mục phụ kiện") @PathVariable Long id,
                                    @Valid @RequestBody AccessoryCategoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Danh mục phụ kiện được cập nhật thành công",
                "Danh mục phụ kiện với mã " + id + " đã cập nhật vào hệ thống",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa danh mục phụ kiện", description = "Xóa danh mục phụ kiện theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa danh mục phụ kiện thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục phụ kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống nội bộ")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Mã danh mục phụ kiện") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Danh mục phụ kiện được xóa thành công",
                "Danh mục phụ kiện với mã " + id + " đã xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }
}
*/

