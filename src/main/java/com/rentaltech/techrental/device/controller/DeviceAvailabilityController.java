package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.rentalorder.service.BookingCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/devices/models")
@Tag(name = "Tra cứu khả dụng thiết bị", description = "Các API kiểm tra số lượng thiết bị còn trống theo model trong một khoảng thời gian")
@RequiredArgsConstructor
public class DeviceAvailabilityController {

    private final BookingCalendarService bookingCalendarService;
    public static final long AVAILABILITY_MARGIN_DAYS = 2L;

    @GetMapping("/{deviceModelId}/availability")
    @Operation(summary = "Số lượng thiết bị còn trống theo model", description = "Trả về số thiết bị của một model có thể sử dụng trong khoảng thời gian yêu cầu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin khả dụng"),
            @ApiResponse(responseCode = "400", description = "Khoảng thời gian hoặc tham số không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getAvailability(@PathVariable Long deviceModelId,
                                             @RequestParam("start") LocalDateTime start,
                                             @RequestParam("end") LocalDateTime end) {
        LocalDateTime marginStart = start.minusDays(AVAILABILITY_MARGIN_DAYS);
        LocalDateTime marginEnd = end.plusDays(AVAILABILITY_MARGIN_DAYS);
        long available = bookingCalendarService.getAvailableCountByModel(deviceModelId, marginStart, marginEnd);
        var body = AvailabilityResponse.builder()
                .deviceModelId(deviceModelId)
                .start(start)
                .end(end)
                .available(available)
                .build();
        return ResponseUtil.createSuccessResponse(
                "Khả dụng theo model",
                "Số lượng thiết bị còn trống trong khoảng thời gian yêu cầu",
                body,
                HttpStatus.OK
        );
    }

    @GetMapping("/{deviceModelId}/availability/check")
    @Operation(summary = "Kiểm tra đáp ứng nhu cầu thiết bị", description = "Kiểm tra số lượng yêu cầu có thể đáp ứng với model và khoảng thời gian cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kết quả kiểm tra số lượng khả dụng"),
            @ApiResponse(responseCode = "400", description = "Giá trị kiểm tra không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> checkAvailability(@PathVariable Long deviceModelId,
                                               @RequestParam("start") LocalDateTime start,
                                               @RequestParam("end") LocalDateTime end,
                                               @RequestParam("quantity") long quantity) {
        LocalDateTime marginStart = start.minusDays(AVAILABILITY_MARGIN_DAYS);
        LocalDateTime marginEnd = end.plusDays(AVAILABILITY_MARGIN_DAYS);
        long available = bookingCalendarService.getAvailableCountByModel(deviceModelId, marginStart, marginEnd);
        boolean canFulfill = quantity <= available;
        var body = CheckAvailabilityResponse.builder()
                .deviceModelId(deviceModelId)
                .start(start)
                .end(end)
                .requested(quantity)
                .available(available)
                .canFulfill(canFulfill)
                .build();
        return ResponseUtil.createSuccessResponse(
                canFulfill ? "Đủ khả năng đáp ứng" : "Không đủ thiết bị để đáp ứng",
                canFulfill ? "Số lượng yêu cầu nằm trong giới hạn thiết bị còn trống"
                        : "Số lượng yêu cầu vượt quá số thiết bị khả dụng",
                body,
                HttpStatus.OK
        );
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class AvailabilityResponse {
        private Long deviceModelId;
        private LocalDateTime start;
        private LocalDateTime end;
        private long available;
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class CheckAvailabilityResponse {
        private Long deviceModelId;
        private LocalDateTime start;
        private LocalDateTime end;
        private long requested;
        private long available;
        private boolean canFulfill;
    }
}

