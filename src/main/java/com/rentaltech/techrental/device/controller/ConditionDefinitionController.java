package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.ConditionDefinitionRequestDto;
import com.rentaltech.techrental.device.service.ConditionDefinitionService;
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
@RequestMapping("/api/conditions/definitions")
@RequiredArgsConstructor
@Tag(name = "Định nghĩa tình trạng thiết bị", description = "Nhóm API quản lý danh sách tiêu chí đánh giá tình trạng thiết bị")
public class ConditionDefinitionController {

    private final ConditionDefinitionService conditionDefinitionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Tạo định nghĩa tình trạng", description = "Thêm mới tiêu chí đánh giá tình trạng thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo định nghĩa tình trạng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> create(@Valid @RequestBody ConditionDefinitionRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Tạo định nghĩa tình trạng thành công",
                "Định nghĩa tình trạng mới đã được thêm vào hệ thống",
                conditionDefinitionService.create(request),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Cập nhật định nghĩa tình trạng", description = "Chỉnh sửa nội dung tiêu chí đánh giá")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy định nghĩa tình trạng"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody ConditionDefinitionRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật định nghĩa tình trạng thành công",
                "Định nghĩa tình trạng đã được cập nhật",
                conditionDefinitionService.update(id, request),
                HttpStatus.OK
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết định nghĩa tình trạng", description = "Trả về thông tin định nghĩa tình trạng theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin định nghĩa tình trạng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy định nghĩa tình trạng"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Thông tin định nghĩa tình trạng",
                "Chi tiết định nghĩa tình trạng",
                conditionDefinitionService.getById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách định nghĩa tình trạng", description = "Có thể lọc theo mã model thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách định nghĩa tình trạng"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getAll(@RequestParam(required = false) Long deviceModelId) {
        var data = deviceModelId == null
                ? conditionDefinitionService.getAll()
                : conditionDefinitionService.getByDeviceModel(deviceModelId);
        return ResponseUtil.createSuccessResponse(
                "Danh sách định nghĩa tình trạng",
                "Toàn bộ định nghĩa tình trạng hiện có",
                data,
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa định nghĩa tình trạng", description = "Xóa tiêu chí đánh giá tình trạng khỏi hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy định nghĩa tình trạng"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        conditionDefinitionService.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Định nghĩa tình trạng đã bị xóa",
                "Bản ghi định nghĩa tình trạng không còn trong hệ thống",
                HttpStatus.NO_CONTENT
        );
    }
}
