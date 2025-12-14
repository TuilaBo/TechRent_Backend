package com.rentaltech.techrental.webapi.technician.service;

import com.rentaltech.techrental.webapi.technician.model.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QCReportService {
    QCReportResponseDto createPreRentalReport(QCReportPreRentalCreateRequestDto request, MultipartFile accessorySnapshot, String username);

    QCReportResponseDto createPostRentalReport(QCReportPostRentalCreateRequestDto request, MultipartFile accessorySnapshot, String username);

    QCReportResponseDto getReport(Long reportId);

    QCReportResponseDto updatePreRentalReport(Long reportId, QCReportPreRentalUpdateRequestDto request, MultipartFile accessorySnapshot, String username);

    QCReportResponseDto updatePostRentalReport(Long reportId, QCReportPostRentalUpdateRequestDto request, MultipartFile accessorySnapshot, String username);

    List<QCReportResponseDto> getReportsByOrder(Long rentalOrderId);
}
