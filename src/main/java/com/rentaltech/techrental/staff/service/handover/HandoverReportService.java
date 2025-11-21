package com.rentaltech.techrental.staff.service.handover;

import com.rentaltech.techrental.staff.model.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HandoverReportService {

    HandoverReportResponseDto createReport(HandoverReportCreateRequestDto request, List<MultipartFile> evidences, String staffUsername);

    HandoverPinDeliveryDto sendPinForOrder(Long orderId);

    HandoverReportResponseDto getReport(Long handoverReportId);

    List<HandoverReportResponseDto> getReportsByTask(Long taskId);

    List<HandoverReportResponseDto> getReportsByOrder(Long orderId);

    List<HandoverReportResponseDto> getReportsByTechnician(Long staffId);

    List<HandoverReportResponseDto> getAllReports();

    HandoverPinDeliveryDto sendPinToStaffForReport(Long handoverReportId);

    HandoverReportResponseDto signByStaff(Long handoverReportId, HandoverReportStaffSignRequestDto request);

    HandoverPinDeliveryDto sendPinToCustomerForReport(Long handoverReportId, String email);

    HandoverReportResponseDto signByCustomer(Long handoverReportId, HandoverReportCustomerSignRequestDto request);

    List<HandoverReportResponseDto> getReportsByCustomerOrder(Long customerId);
}

