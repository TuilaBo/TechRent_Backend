package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.dto.DeviceReplacementReportCustomerSignRequestDto;
import com.rentaltech.techrental.staff.model.dto.DeviceReplacementReportResponseDto;
import com.rentaltech.techrental.staff.model.dto.HandoverPinDeliveryDto;
import com.rentaltech.techrental.staff.service.devicereplacement.DeviceReplacementReportService;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/device-replacement-reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Device Replacement Reports", description = "Khách hàng xem và ký biên bản đổi thiết bị")
public class CustomerDeviceReplacementReportController {

    private final DeviceReplacementReportService replacementReportService;
    private final CustomerRepository customerRepository;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Danh sách biên bản đổi thiết bị của tôi", description = "Khách hàng xem toàn bộ biên bản đổi thiết bị thuộc các đơn hàng của mình")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách biên bản đổi thiết bị"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> getMyReplacementReports(@AuthenticationPrincipal UserDetails principal) {
        Customer customer = getCustomerFromPrincipal(principal);
        List<DeviceReplacementReportResponseDto> reports = replacementReportService.getReportsByCustomerOrder(customer.getCustomerId());
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách biên bản đổi thiết bị thành công",
                "Danh sách biên bản đổi thiết bị của các đơn hàng của bạn",
                reports,
                HttpStatus.OK
        );
    }

    @GetMapping("/{replacementReportId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Chi tiết biên bản đổi thiết bị", description = "Khách hàng xem chi tiết một biên bản đổi thiết bị cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về chi tiết biên bản đổi thiết bị"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xem biên bản này"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy biên bản")
    })
    public ResponseEntity<?> getReplacementReport(@PathVariable Long replacementReportId, @AuthenticationPrincipal UserDetails principal) {
        getCustomerFromPrincipal(principal); // Verify customer exists
        DeviceReplacementReportResponseDto report = replacementReportService.getReport(replacementReportId);
        
        // Validate customer có quyền xem biên bản này
        if (report.getOrderId() == null) {
            throw new IllegalStateException("Biên bản không gắn với đơn hàng");
        }
        // Có thể thêm validation nếu cần
        
        return ResponseUtil.createSuccessResponse(
                "Lấy chi tiết biên bản đổi thiết bị thành công",
                "Chi tiết biên bản đổi thiết bị",
                report,
                HttpStatus.OK
        );
    }

    @PostMapping("/{replacementReportId}/pin")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Gửi PIN cho khách hàng", description = "Gửi mã PIN xác nhận cho khách qua SMS/Email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gửi PIN thành công"),
            @ApiResponse(responseCode = "400", description = "Email không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy biên bản"),
            @ApiResponse(responseCode = "500", description = "Không thể gửi PIN do lỗi hệ thống")
    })
    public ResponseEntity<?> requestPinForCustomer(
            @PathVariable Long replacementReportId,
            @RequestParam(required = false) String email) {
        HandoverPinDeliveryDto responseDto = replacementReportService.sendPinToCustomerForReport(replacementReportId, email);
        return ResponseUtil.createSuccessResponse(
                "Gửi PIN thành công",
                "Khách hàng sẽ nhận PIN qua SMS/Email nếu khả dụng",
                responseDto,
                HttpStatus.OK
        );
    }

    @PatchMapping("/{replacementReportId}/signature")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách hàng ký biên bản", description = "Xác nhận biên bản đổi thiết bị bằng mã PIN của khách hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ký biên bản thành công"),
            @ApiResponse(responseCode = "400", description = "Mã PIN hoặc dữ liệu ký không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy biên bản"),
            @ApiResponse(responseCode = "500", description = "Không thể ký do lỗi hệ thống")
    })
    public ResponseEntity<?> signByCustomer(
            @PathVariable Long replacementReportId,
            @Valid @RequestBody DeviceReplacementReportCustomerSignRequestDto request) {
        DeviceReplacementReportResponseDto responseDto = replacementReportService.signByCustomer(replacementReportId, request);
        return ResponseUtil.createSuccessResponse(
                "Ký biên bản thành công",
                "Bạn đã ký biên bản đổi thiết bị",
                responseDto,
                HttpStatus.OK
        );
    }

    private Customer getCustomerFromPrincipal(UserDetails principal) {
        String username = principal.getUsername();
        return customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin khách hàng"));
    }
}

