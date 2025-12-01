package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.ChatMessage;
import com.rentaltech.techrental.staff.model.ChatMessageSenderType;
import com.rentaltech.techrental.staff.model.Conversation;
import com.rentaltech.techrental.staff.model.dto.ChatMessageCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.ChatMessageResponseDto;
import com.rentaltech.techrental.staff.service.chatservice.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Trò chuyện hỗ trợ", description = "API phục vụ chat giữa khách hàng và nhân viên CSKH")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversations/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Lấy/tạo conversation theo khách hàng", description = "Lấy conversation hiện có hoặc tạo mới cho khách hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy conversation thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng"),
            @ApiResponse(responseCode = "500", description = "Không thể xử lý do lỗi hệ thống")
    })
    public ResponseEntity<?> getOrCreateConversationByCustomer(@PathVariable Long customerId) {
        Conversation conversation = chatService.getOrCreateConversationByCustomer(customerId);
        return ResponseUtil.createSuccessResponse(
                "Lấy conversation thành công",
                "Conversation theo customer ID",
                mapConversationToDto(conversation),
                HttpStatus.OK
        );
    }

    @PostMapping("/messages")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Gửi tin nhắn", description = "Gửi tin nhắn chat giữa khách hàng và nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Gửi tin nhắn thành công"),
            @ApiResponse(responseCode = "400", description = "Nội dung tin nhắn không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể gửi tin nhắn do lỗi hệ thống")
    })
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatMessageCreateRequestDto request) {
        ChatMessage message = chatService.sendMessage(request);
        return ResponseUtil.createSuccessResponse(
                "Gửi tin nhắn thành công",
                "Tin nhắn đã được gửi",
                ChatMessageResponseDto.from(message),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/messages/conversation/{conversationId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Danh sách tin nhắn", description = "Lấy tin nhắn trong conversation có phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách tin nhắn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy conversation"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Tạo Pageable với default sort (không cần sort vì repository đã có OrderBySentAtDesc)
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var pageResult = chatService.getMessages(conversationId, pageable);
        var pageDto = pageResult.map(ChatMessageResponseDto::from);
        return ResponseUtil.createSuccessPaginationResponse(
                "Danh sách tin nhắn",
                "Tin nhắn trong conversation với phân trang",
                pageDto,
                HttpStatus.OK
        );
    }

    @PostMapping("/messages/conversation/{conversationId}/mark-read")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Đánh dấu tin nhắn đã đọc", description = "Đánh dấu toàn bộ tin chưa đọc trong conversation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đánh dấu đã đọc thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy conversation"),
            @ApiResponse(responseCode = "500", description = "Không thể cập nhật do lỗi hệ thống")
    })
    public ResponseEntity<?> markAsRead(@PathVariable Long conversationId, @RequestBody MarkReadRequest request) {
        chatService.markMessagesAsRead(conversationId, request.getUserId(), request.getUserType());
        return ResponseUtil.createSuccessResponse(
                "Đánh dấu đã đọc thành công",
                "Tất cả tin nhắn chưa đọc đã được đánh dấu",
                null,
                HttpStatus.OK
        );
    }

    @GetMapping("/messages/conversation/{conversationId}/unread-count")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Đếm tin nhắn chưa đọc", description = "Lấy số lượng tin chưa đọc cho người dùng hiện tại")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về số lượng tin chưa đọc"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getUnreadCount(@PathVariable Long conversationId,
                                           @RequestParam Long userId,
                                           @RequestParam ChatMessageSenderType userType) {
        Long count = chatService.getUnreadCount(conversationId, userId, userType);
        return ResponseUtil.createSuccessResponse(
                "Số lượng tin nhắn chưa đọc",
                "",
                count,
                HttpStatus.OK
        );
    }

    @GetMapping("/conversations/staff/{staffId}")
    @PreAuthorize("hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Conversations theo nhân viên CSKH", description = "Danh sách conversation được gán cho nhân viên, có phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách conversation"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getConversationsByStaff(
            @PathVariable Long staffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var pageResult = chatService.getConversationsByStaff(staffId, pageable);
        var pageDto = pageResult.map(this::mapConversationToDto);
        return ResponseUtil.createSuccessPaginationResponse(
                "Danh sách conversations",
                "Conversations được assign cho staff",
                pageDto,
                HttpStatus.OK
        );
    }


    private ConversationResponseDto mapConversationToDto(Conversation conversation) {
        return ConversationResponseDto.builder()
                .conversationId(conversation.getConversationId())
                .customerId(conversation.getCustomer() != null ? conversation.getCustomer().getCustomerId() : null)
                .staffId(conversation.getStaff() != null ? conversation.getStaff().getStaffId() : null)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConversationResponseDto {
        private Long conversationId;
        private Long customerId;
        private Long staffId;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }

    @Data
    public static class MarkReadRequest {
        private Long userId;
        private ChatMessageSenderType userType;
    }
}

