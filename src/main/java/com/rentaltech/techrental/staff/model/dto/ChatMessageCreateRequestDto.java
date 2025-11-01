package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.ChatMessageSenderType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageCreateRequestDto {
    @NotNull
    private Long conversationId;
    
    @NotNull
    private ChatMessageSenderType senderType;
    
    @NotNull
    private Long senderId;  // customerId nếu senderType = CUSTOMER, staffId nếu = STAFF
    
    @NotNull
    private String content;
}

