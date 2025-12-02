package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.customer.model.dto.NotificationResponseDto;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Thông báo", description = "API truy vấn thông báo cho người dùng hệ thống")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/accounts/{accountId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Lấy thông báo theo tài khoản", description = "Trả về danh sách thông báo thuộc tài khoản cụ thể")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trả về danh sách thông báo"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getNotificationsByAccountId(@PathVariable Long accountId) {
        List<NotificationResponseDto> notifications = notificationService.getNotificationsForAccount(accountId);
        log.info("Fetched {} notifications for accountId {}", notifications.size(), accountId);

        return ResponseUtil.createSuccessResponse(
                "Lấy thông báo thành công",
                "Danh sách thông báo cho accountId " + accountId,
                notifications,
                HttpStatus.OK
        );
    }
}
