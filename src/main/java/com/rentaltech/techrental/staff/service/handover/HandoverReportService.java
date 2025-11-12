package com.rentaltech.techrental.staff.service.handover;

import com.rentaltech.techrental.staff.model.dto.HandoverPinDeliveryDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HandoverReportService {

    HandoverReportResponseDto createReport(HandoverReportCreateRequestDto request, List<MultipartFile> evidences);

    HandoverPinDeliveryDto sendPinForOrder(Long orderId);

    HandoverReportResponseDto getReport(Long handoverReportId);

    List<HandoverReportResponseDto> getReportsByTask(Long taskId);

    List<HandoverReportResponseDto> getReportsByOrder(Long orderId);

    List<HandoverReportResponseDto> getReportsByTechnician(Long staffId);

    List<HandoverReportResponseDto> getAllReports();
}

