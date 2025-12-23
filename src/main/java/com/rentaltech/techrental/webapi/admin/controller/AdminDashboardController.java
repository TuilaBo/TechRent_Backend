package com.rentaltech.techrental.webapi.admin.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.admin.service.AdminDashboardService;
import com.rentaltech.techrental.webapi.admin.service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "Các API thống kê tổng quan dành cho admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/new-customers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Số khách hàng đăng ký mới", description = "Đếm số khách hàng mới trong tháng (theo createdAt của Customer)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về số khách hàng mới"),
            @ApiResponse(responseCode = "500", description = "Không thể thống kê do lỗi hệ thống")
    })
    public ResponseEntity<?> getNewCustomers(
            @RequestParam int year,
            @RequestParam int month
    ) {
        NewCustomerStatsDto stats = dashboardService.getNewCustomers(year, month);
        return ResponseUtil.createSuccessResponse(
                "Thống kê khách hàng mới",
                "Số khách hàng đăng ký mới trong tháng",
                stats,
                HttpStatus.OK
        );
    }

    @GetMapping("/device-imports-by-category")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Số thiết bị nhập theo category", description = "Đếm số thiết bị có acquireAt trong tháng, group theo category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thống kê thiết bị nhập"),
            @ApiResponse(responseCode = "500", description = "Không thể thống kê do lỗi hệ thống")
    })
    public ResponseEntity<?> getDeviceImportsByCategory(
            @RequestParam int year,
            @RequestParam int month
    ) {
        DeviceImportByCategoryStatsDto stats = dashboardService.getDeviceImportsByCategory(year, month);
        return ResponseUtil.createSuccessResponse(
                "Thống kê thiết bị nhập trong tháng",
                "Số lượng thiết bị nhập theo category",
                stats,
                HttpStatus.OK
        );
    }

    @GetMapping("/device-incidents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thiết bị hư hỏng / mất mát trong tháng", description = "Thống kê số báo cáo sai khác DAMAGE và MISSING_ITEM trong tháng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thống kê thiết bị hư hỏng/mất mát"),
            @ApiResponse(responseCode = "500", description = "Không thể thống kê do lỗi hệ thống")
    })
    public ResponseEntity<?> getDeviceIncidents(
            @RequestParam int year,
            @RequestParam int month
    ) {
        DeviceIncidentStatsDto stats = dashboardService.getDeviceIncidents(year, month);
        return ResponseUtil.createSuccessResponse(
                "Thống kê thiết bị hư hỏng / mất mát",
                "Theo loại sai khác trong tháng",
                stats,
                HttpStatus.OK
        );
    }

    @GetMapping("/damages")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê thiệt hại", description = "Tổng hợp thiệt hại trong tháng từ DiscrepancyReport.penaltyAmount và Settlement.damageFee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thống kê thiệt hại"),
            @ApiResponse(responseCode = "500", description = "Không thể thống kê do lỗi hệ thống")
    })
    public ResponseEntity<?> getDamageStats(
            @RequestParam int year,
            @RequestParam int month
    ) {
        DamageStatsDto stats = dashboardService.getDamageStats(year, month);
        return ResponseUtil.createSuccessResponse(
                "Thống kê thiệt hại trong tháng",
                "Tổng hợp theo discrepancy và settlement",
                stats,
                HttpStatus.OK
        );
    }

    @GetMapping("/orders-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê đơn hàng hoàn thành / hủy", description = "Đếm số đơn COMPLETED (theo endDate) và CANCELLED (theo createdAt) trong tháng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thống kê trạng thái đơn hàng"),
            @ApiResponse(responseCode = "500", description = "Không thể thống kê do lỗi hệ thống")
    })
    public ResponseEntity<?> getOrderStatusStats(
            @RequestParam int year,
            @RequestParam int month
    ) {
        OrderStatusStatsDto stats = dashboardService.getOrderStatusStats(year, month);
        return ResponseUtil.createSuccessResponse(
                "Thống kê đơn hàng theo trạng thái",
                "Số lượng đơn COMPLETED và CANCELLED trong tháng",
                stats,
                HttpStatus.OK
        );
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê doanh thu",
            description = "Tính doanh thu theo ngày/tháng/năm. Bao gồm: tiền thuê, tiền phạt trả muộn, tiền bồi thường thiệt hại. KHÔNG tính tiền cọc.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thống kê doanh thu"),
            @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể thống kê do lỗi hệ thống")
    })
    public ResponseEntity<?> getRevenueStats(
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day
    ) {
        if (day != null && month == null) {
            return ResponseUtil.createErrorResponse(
                    "INVALID_PARAMETERS",
                    "Tham số không hợp lệ",
                    "Không thể query theo ngày mà không có tháng",
                    HttpStatus.BAD_REQUEST
            );
        }
        RevenueStatsDto stats = dashboardService.getRevenueStats(year, month, day);
        String message = day != null
                ? "Doanh thu trong ngày"
                : (month != null ? "Doanh thu trong tháng" : "Doanh thu trong năm");
        return ResponseUtil.createSuccessResponse(
                "Thống kê doanh thu",
                message,
                stats,
                HttpStatus.OK
        );
    }
}


