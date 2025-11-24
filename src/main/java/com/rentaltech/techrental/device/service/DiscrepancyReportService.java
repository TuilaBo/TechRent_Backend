package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportRequestDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportResponseDto;

import java.util.List;

public interface DiscrepancyReportService {
    DiscrepancyReportResponseDto create(DiscrepancyReportRequestDto request);
    DiscrepancyReportResponseDto update(Long id, DiscrepancyReportRequestDto request);
    DiscrepancyReportResponseDto getById(Long id);
    List<DiscrepancyReportResponseDto> getByReference(DiscrepancyCreatedFrom createdFrom, Long refId);
    List<DiscrepancyReportResponseDto> getAll();
}
