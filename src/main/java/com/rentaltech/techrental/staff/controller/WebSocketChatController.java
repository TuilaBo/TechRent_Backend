package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.staff.model.ChatMessage;
import com.rentaltech.techrental.staff.model.ChatMessageSenderType;
import com.rentaltech.techrental.staff.model.dto.ChatMessageResponseDto;
import com.rentaltech.techrental.staff.service.chatservice.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessagePayload payload) {
        log.info("Received message: {}", payload);

        com.rentaltech.techrental.staff.model.dto.ChatMessageCreateRequestDto createDto =
                com.rentaltech.techrental.staff.model.dto.ChatMessageCreateRequestDto.builder()
                        .conversationId(payload.getConversationId())
                        .senderType(payload.getSenderType())
                        .senderId(payload.getSenderId())
                        .content(payload.getContent())
                        .build();

        ChatMessage message = chatService.sendMessage(createDto);
        ChatMessageResponseDto response = mapToResponseDto(message);

        // Gửi đến conversation cụ thể để cả customer và staff đều nhận được
        messagingTemplate.convertAndSend("/topic/conversation/" + payload.getConversationId(), response);
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

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatMessagePayload {
        private Long conversationId;
        private ChatMessageSenderType senderType;
        private Long senderId;
        private String content;
    }
}

