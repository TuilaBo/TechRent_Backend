package com.rentaltech.techrental.staff.model.dto;

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
}

