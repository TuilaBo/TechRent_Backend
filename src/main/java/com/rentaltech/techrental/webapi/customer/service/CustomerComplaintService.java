package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.ComplaintStatus;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintResponseDto;

import java.util.List;

public interface CustomerComplaintService {
    
    /**
     * Customer tạo khiếu nại về thiết bị bị hỏng
     */
    CustomerComplaintResponseDto createComplaint(CustomerComplaintRequestDto request, String username);
    
    /**
     * Customer xem danh sách khiếu nại của mình
     */
    List<CustomerComplaintResponseDto> getMyComplaints(String username);
    
    /**
     * Customer xem chi tiết khiếu nại
     */
    CustomerComplaintResponseDto getComplaintById(Long complaintId, String username);
    
    /**
     * Customer xem khiếu nại theo order
     */
    List<CustomerComplaintResponseDto> getComplaintsByOrder(Long orderId, String username);
    
    /**
     * Staff xem tất cả khiếu nại (có thể filter theo status)
     */
    List<CustomerComplaintResponseDto> getAllComplaints(ComplaintStatus status);
    
    /**
     * Staff xem chi tiết khiếu nại
     */
    CustomerComplaintResponseDto getComplaintById(Long complaintId);
    
    /**
     * Staff xử lý khiếu nại: Tự động tìm device thay thế, tạo allocation mới, tạo task
     */
    CustomerComplaintResponseDto processComplaint(Long complaintId, String staffNote, String username);
    
    /**
     * Staff đánh dấu đã giải quyết (sau khi hoàn thành task đổi máy)
     */
    CustomerComplaintResponseDto resolveComplaint(Long complaintId, String staffNote, String username);
    
    /**
     * Staff hủy khiếu nại (nếu không cần thay thế)
     */
    CustomerComplaintResponseDto cancelComplaint(Long complaintId, String staffNote, String username);
}

