package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.rentalorder.service.BookingCalendarService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Device Availability", description = "Availability checks by device model")
@RequiredArgsConstructor
public class DeviceAvailabilityController {

    private final BookingCalendarService bookingCalendarService;
    public static final long AVAILABILITY_MARGIN_DAYS = 2L;

    @GetMapping("/{deviceModelId}/availability")
    @Operation(summary = "Get available count by model within time range")
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
                "Availability by model",
                "Number of devices available in the given time range",
                body,
                HttpStatus.OK
        );
    }

    @GetMapping("/{deviceModelId}/availability/check")
    @Operation(summary = "Check if requested quantity is available by model within time range")
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
                canFulfill ? "Can fulfill request" : "Insufficient availability",
                canFulfill ? "Requested quantity is available" : "Requested quantity exceeds available devices",
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

