package com.rentaltech.techrental.webapi.technician.service;

import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPostRentalCreateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportPreRentalCreateRequestDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportResponseDto;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportUpdateRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QCReportService {
    QCReportResponseDto createPreRentalReport(QCReportPreRentalCreateRequestDto request, MultipartFile accessorySnapshot, String username);

    QCReportResponseDto createPostRentalReport(QCReportPostRentalCreateRequestDto request, MultipartFile accessorySnapshot, String username);

    QCReportResponseDto getReport(Long reportId);

    QCReportResponseDto updateReport(Long reportId, QCReportUpdateRequestDto request, MultipartFile accessorySnapshot, String username);

    List<QCReportResponseDto> getReportsByOrder(Long rentalOrderId);
}
