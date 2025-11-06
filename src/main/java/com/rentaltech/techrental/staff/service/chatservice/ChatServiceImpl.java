package com.rentaltech.techrental.staff.service.chatservice;

import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.model.dto.ChatMessageCreateRequestDto;
import com.rentaltech.techrental.staff.repository.ChatMessageRepository;
import com.rentaltech.techrental.staff.repository.ConversationRepository;
import com.rentaltech.techrental.staff.repository.DisputeRepository;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DisputeRepository disputeRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    @Override
    public Conversation getOrCreateConversationByDispute(Long disputeId) {
        return conversationRepository.findByDispute_DisputeId(disputeId)
                .orElseGet(() -> {
                    Dispute dispute = disputeRepository.findById(disputeId)
                            .orElseThrow(() -> new NoSuchElementException("Dispute not found: " + disputeId));

                    Customer customer = dispute.getOpenedByCustomer();
                    if (customer == null) {
                        throw new IllegalStateException("Dispute must have a customer to create conversation");
                    }

                    Staff staff = findAvailableSupportStaff();

                    Conversation conversation = Conversation.builder()
                            .dispute(dispute)
                            .customer(customer)
                            .staff(staff)
                            .build();

                    return conversationRepository.save(conversation);
                });
    }

    @Override
    public Conversation getOrCreateConversationByCustomer(Long customerId) {
        return conversationRepository.findByCustomer_CustomerId(customerId)
                .orElseGet(() -> {
                    Customer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

                    Staff staff = findAvailableSupportStaff();

                    Conversation conversation = Conversation.builder()
                            .dispute(null)
                            .customer(customer)
                            .staff(staff)
                            .build();

                    return conversationRepository.save(conversation);
                });
    }

    private Staff findAvailableSupportStaff() {
        // Tìm CUSTOMER_SUPPORT_STAFF trước
        List<Staff> supportStaffs = staffRepository.findByStaffRoleAndIsActiveTrue(StaffRole.CUSTOMER_SUPPORT_STAFF);
        if (!supportStaffs.isEmpty()) {
            return supportStaffs.get(0); // Lấy staff đầu tiên, có thể random hoặc round-robin
        }
        
        // Nếu không có CUSTOMER_SUPPORT_STAFF, fallback sang ADMIN
        List<Staff> adminStaffs = staffRepository.findByStaffRoleAndIsActiveTrue(StaffRole.ADMIN);
        if (!adminStaffs.isEmpty()) {
            return adminStaffs.get(0);
        }
        
        // Nếu không có ADMIN, fallback sang OPERATOR
        List<Staff> operatorStaffs = staffRepository.findByStaffRoleAndIsActiveTrue(StaffRole.OPERATOR);
        if (!operatorStaffs.isEmpty()) {
            return operatorStaffs.get(0);
        }
        
        // Nếu vẫn không có, lấy bất kỳ staff active nào
        List<Staff> anyActiveStaffs = staffRepository.findByIsActiveTrue();
        if (!anyActiveStaffs.isEmpty()) {
            return anyActiveStaffs.get(0);
        }
        
        // Cuối cùng mới throw error
        throw new IllegalStateException("No active staff found in the system");
    }

    @Override
    public ChatMessage sendMessage(ChatMessageCreateRequestDto request) {
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new NoSuchElementException("Conversation not found: " + request.getConversationId()));

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .senderType(request.getSenderType())
                .senderId(request.getSenderId())
                .content(request.getContent())
                .isRead(false)
                .readAt(null)
                .sentAt(LocalDateTime.now())
                .build();

        return chatMessageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessage> getMessages(Long conversationId, Pageable pageable) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new NoSuchElementException("Conversation not found: " + conversationId);
        }
        return chatMessageRepository.findByConversation_ConversationIdOrderBySentAtDesc(conversationId, pageable);
    }

    @Override
    public void markMessagesAsRead(Long conversationId, Long currentUserId, ChatMessageSenderType currentUserType) {
        List<ChatMessage> unreadMessages = chatMessageRepository.findByConversation_ConversationIdAndIsReadFalse(conversationId);
        
        for (ChatMessage message : unreadMessages) {
            // Chỉ mark as read nếu message không phải do chính người này gửi
            if (!message.getSenderId().equals(currentUserId) || 
                message.getSenderType() != currentUserType) {
                message.setIsRead(true);
                message.setReadAt(LocalDateTime.now());
            }
        }
        
        chatMessageRepository.saveAll(unreadMessages);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long conversationId, Long currentUserId, ChatMessageSenderType currentUserType) {
        List<ChatMessage> allUnread = chatMessageRepository.findByConversation_ConversationIdAndIsReadFalse(conversationId);
        
        return allUnread.stream()
                .filter(msg -> !msg.getSenderId().equals(currentUserId) || msg.getSenderType() != currentUserType)
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> getConversationsByStaff(Long staffId, Pageable pageable) {
        return conversationRepository.findByStaff_StaffId(staffId, pageable);
    }
}

