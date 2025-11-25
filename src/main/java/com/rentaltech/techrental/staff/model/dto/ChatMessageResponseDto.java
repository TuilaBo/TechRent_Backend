package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.ChatMessage;
import com.rentaltech.techrental.staff.model.ChatMessageSenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDto {
    private Long messageId;
    private Long conversationId;
    private ChatMessageSenderType senderType;
    private Long senderId;
    private String content;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;

    public static ChatMessageResponseDto from(ChatMessage message) {
        if (message == null) {
            return null;
        }
        var conversation = message.getConversation();
        return ChatMessageResponseDto.builder()
                .messageId(message.getMessageId())
                .conversationId(conversation != null ? conversation.getConversationId() : null)
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .sentAt(message.getSentAt())
                .build();
    }
}

