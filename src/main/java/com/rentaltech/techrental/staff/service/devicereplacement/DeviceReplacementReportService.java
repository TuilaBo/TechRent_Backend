package com.rentaltech.techrental.staff.service.devicereplacement;

import com.rentaltech.techrental.staff.model.dto.DeviceReplacementReportCustomerSignRequestDto;
import com.rentaltech.techrental.staff.model.dto.DeviceReplacementReportResponseDto;
import com.rentaltech.techrental.staff.model.dto.DeviceReplacementReportStaffSignRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverPinDeliveryDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DeviceReplacementReportService {

    /**
     * Tạo biên bản đổi thiết bị cho customer complaint
     * Bao gồm device cũ và device mới
     */
    DeviceReplacementReportResponseDto createDeviceReplacementReport(Long complaintId, String staffUsername);

    DeviceReplacementReportResponseDto getReport(Long replacementReportId);

    List<DeviceReplacementReportResponseDto> getReportsByTask(Long taskId);

    List<DeviceReplacementReportResponseDto> getReportsByOrder(Long orderId);

    List<DeviceReplacementReportResponseDto> getReportsByStaff(Long staffId);

    List<DeviceReplacementReportResponseDto> getAllReports();

    HandoverPinDeliveryDto sendPinToStaffForReport(Long replacementReportId);

    DeviceReplacementReportResponseDto signByStaff(Long replacementReportId, DeviceReplacementReportStaffSignRequestDto request);

    HandoverPinDeliveryDto sendPinToCustomerForReport(Long replacementReportId, String email);

    DeviceReplacementReportResponseDto signByCustomer(Long replacementReportId, DeviceReplacementReportCustomerSignRequestDto request);

    List<DeviceReplacementReportResponseDto> getReportsByCustomerOrder(Long customerId);

    com.rentaltech.techrental.staff.model.dto.DeviceReplacementReportItemResponseDto updateEvidenceByDevice(
            Long replacementReportId,
            Long deviceId,
            List<MultipartFile> files,
            String staffUsername);

    /**
     * Tạo DiscrepancyReport cho complaint nếu faultSource = CUSTOMER
     * Được gọi sau khi technician xác định faultSource và conditionDefinitionIds
     */
    void createDiscrepancyReportIfNeeded(Long complaintId);
}

