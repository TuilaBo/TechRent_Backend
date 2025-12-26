package com.rentaltech.techrental.staff.service.chatservice;

import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.model.dto.ChatMessageCreateRequestDto;
import com.rentaltech.techrental.staff.repository.ChatMessageRepository;
import com.rentaltech.techrental.staff.repository.ConversationRepository;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    @Override
    public Conversation getOrCreateConversationByCustomer(Long customerId) {
        return conversationRepository.findByCustomer_CustomerId(customerId)
                .orElseGet(() -> {
                    Customer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

                    Staff staff = findAvailableSupportStaff();

                    Conversation conversation = Conversation.builder()
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
            // Load balancing: chọn staff có ít conversations nhất
            return selectStaffWithLeastConversations(supportStaffs);
        }
        
        // Nếu không có CUSTOMER_SUPPORT_STAFF, fallback sang ADMIN
        List<Staff> adminStaffs = staffRepository.findByStaffRoleAndIsActiveTrue(StaffRole.ADMIN);
        if (!adminStaffs.isEmpty()) {
            return selectStaffWithLeastConversations(adminStaffs);
        }
        
        // Nếu không có ADMIN, fallback sang OPERATOR
        List<Staff> operatorStaffs = staffRepository.findByStaffRoleAndIsActiveTrue(StaffRole.OPERATOR);
        if (!operatorStaffs.isEmpty()) {
            return selectStaffWithLeastConversations(operatorStaffs);
        }
        
        // Nếu vẫn không có, lấy bất kỳ staff active nào
        List<Staff> anyActiveStaffs = staffRepository.findByIsActiveTrue();
        if (!anyActiveStaffs.isEmpty()) {
            return selectStaffWithLeastConversations(anyActiveStaffs);
        }
        
        // Cuối cùng mới throw error
        throw new IllegalStateException("No active staff found in the system");
    }
    
    /**
     * Chọn staff có ít conversations nhất để phân phối đều tải
     * Nếu có nhiều staff có cùng số conversations, chọn ngẫu nhiên trong số đó
     */
    private Staff selectStaffWithLeastConversations(List<Staff> staffs) {
        if (staffs == null || staffs.isEmpty()) {
            throw new IllegalArgumentException("Staff list cannot be empty");
        }
        
        // Nếu chỉ có 1 staff, trả về luôn
        if (staffs.size() == 1) {
            return staffs.get(0);
        }
        
        // Đếm số conversations của mỗi staff
        Map<Long, Long> conversationCounts = new HashMap<>();
        for (Staff staff : staffs) {
            if (staff != null && staff.getStaffId() != null) {
                long count = conversationRepository.findByStaff_StaffId(staff.getStaffId()).size();
                conversationCounts.put(staff.getStaffId(), count);
            }
        }
        
        // Tìm số conversations nhỏ nhất
        long minCount = conversationCounts.values().stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);
        
        // Lọc các staff có số conversations = minCount
        List<Staff> staffsWithMinConversations = staffs.stream()
                .filter(staff -> staff != null && staff.getStaffId() != null)
                .filter(staff -> conversationCounts.get(staff.getStaffId()).equals(minCount))
                .collect(Collectors.toList());
        
        // Nếu có nhiều staff có cùng số conversations nhỏ nhất, chọn ngẫu nhiên
        if (staffsWithMinConversations.size() > 1) {
            Random random = new Random();
            return staffsWithMinConversations.get(random.nextInt(staffsWithMinConversations.size()));
        }
        
        // Trả về staff có ít conversations nhất
        return staffsWithMinConversations.get(0);
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

