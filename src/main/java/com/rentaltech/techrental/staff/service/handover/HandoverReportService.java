package com.rentaltech.techrental.staff.service.handover;

import com.rentaltech.techrental.staff.model.dto.HandoverPinDeliveryDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportCreateInRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportCreateOutRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportCustomerSignRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportItemResponseDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportResponseDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportStaffSignRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportUpdateInRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportUpdateOutRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HandoverReportService {

    HandoverReportResponseDto createCheckoutReport(HandoverReportCreateOutRequestDto request, String staffUsername);

    HandoverReportResponseDto createCheckinReport(HandoverReportCreateInRequestDto request, String staffUsername);

    HandoverReportResponseDto updateCheckoutReport(Long reportId, HandoverReportUpdateOutRequestDto request, String staffUsername);

    HandoverReportResponseDto updateCheckinReport(Long reportId, HandoverReportUpdateInRequestDto request, String staffUsername);

    HandoverReportItemResponseDto addEvidence(Long itemId, MultipartFile file, String staffUsername);

    HandoverReportItemResponseDto updateEvidenceByDevice(Long handoverReportId, Long deviceId, List<MultipartFile> files, String staffUsername);

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