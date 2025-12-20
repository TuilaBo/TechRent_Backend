package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.ComplaintStatus;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerComplaintResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CustomerComplaintService {
    
    /**
     * Customer tạo khiếu nại về thiết bị bị hỏng (có thể kèm ảnh bằng chứng)
     */
    CustomerComplaintResponseDto createComplaint(CustomerComplaintRequestDto request, MultipartFile evidenceImage, String username);
    
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
    CustomerComplaintResponseDto processComplaint(Long complaintId,
                                                  com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource faultSource,
                                                  java.util.List<Long> conditionDefinitionIds,
                                                  String damageNote,
                                                  String staffNote,
                                                  String username);

    /**
     * Sau khi kiểm tra tại chỗ, staff cập nhật nguồn lỗi và condition hư hỏng (nếu có).
     */
    CustomerComplaintResponseDto updateFaultAndConditions(Long complaintId,
                                                          com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource faultSource,
                                                          java.util.List<Long> conditionDefinitionIds,
                                                          String damageNote,
                                                          String staffNote,
                                                          String username);
    
    /**
     * Staff đánh dấu đã giải quyết (sau khi hoàn thành task đổi máy)
     * Tự động update task status thành COMPLETED
     */
    CustomerComplaintResponseDto resolveComplaint(Long complaintId, String staffNote, List<org.springframework.web.multipart.MultipartFile> evidenceFiles, String username);
    
    /**
     * Staff hủy khiếu nại (nếu không cần thay thế)
     */
    CustomerComplaintResponseDto cancelComplaint(Long complaintId, String staffNote, String username);
}

