package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.ChatMessage;
import com.rentaltech.techrental.staff.model.ChatMessageSenderType;
import com.rentaltech.techrental.staff.model.Conversation;
import com.rentaltech.techrental.staff.model.dto.ChatMessageCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.ChatMessageResponseDto;
import com.rentaltech.techrental.staff.service.chatservice.ChatService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Chat", description = "Chat message APIs for Customer and Customer Support Staff")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversations/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Get or create conversation by customer", description = "Get existing conversation or create new one for customer")
    public ResponseEntity<?> getOrCreateConversationByCustomer(@PathVariable Long customerId) {
        Conversation conversation = chatService.getOrCreateConversationByCustomer(customerId);
        return ResponseUtil.createSuccessResponse(
                "Lấy conversation thành công",
                "Conversation theo customer ID",
                mapConversationToDto(conversation),
                HttpStatus.OK
        );
    }

//    @PostMapping("/conversations/dispute/{disputeId}")
//    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
//    @Operation(summary = "Get or create conversation by dispute", description = "Get existing conversation or create new one for a dispute")
//    public ResponseEntity<?> getOrCreateConversationByDispute(@PathVariable Long disputeId) {
//        Conversation conversation = chatService.getOrCreateConversationByDispute(disputeId);
//        return ResponseUtil.createSuccessResponse(
//                "Lấy conversation thành công",
//                "Conversation theo dispute ID",
//                mapConversationToDto(conversation),
//                HttpStatus.OK
//        );
//    }

    @PostMapping("/messages")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Send message", description = "Send a chat message")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatMessageCreateRequestDto request) {
        ChatMessage message = chatService.sendMessage(request);
        return ResponseUtil.createSuccessResponse(
                "Gửi tin nhắn thành công",
                "Tin nhắn đã được gửi",
                mapToResponseDto(message),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/messages/conversation/{conversationId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Get messages", description = "Get messages from a conversation with pagination")
    public ResponseEntity<?> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Tạo Pageable với default sort (không cần sort vì repository đã có OrderBySentAtDesc)
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var pageResult = chatService.getMessages(conversationId, pageable);
        var pageDto = pageResult.map(this::mapToResponseDto);
        return ResponseUtil.createSuccessPaginationResponse(
                "Danh sách tin nhắn",
                "Tin nhắn trong conversation với phân trang",
                pageDto,
                HttpStatus.OK
        );
    }

    @PostMapping("/messages/conversation/{conversationId}/mark-read")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Mark messages as read", description = "Mark all unread messages in conversation as read")
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
    @Operation(summary = "Get unread count", description = "Get count of unread messages for current user")
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
    @Operation(summary = "Get conversations by staff", description = "Get all conversations assigned to a staff member with pagination")
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

    private ChatMessageResponseDto mapToResponseDto(ChatMessage message) {
        return ChatMessageResponseDto.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversation() != null ? message.getConversation().getConversationId() : null)
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .sentAt(message.getSentAt())
                .build();
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

