package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.ConditionDefinition;
import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.DiscrepancyReport;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportRequestDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportResponseDto;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.ConditionDefinitionRepository;
import com.rentaltech.techrental.device.repository.DiscrepancyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class DiscrepancyReportServiceImpl implements DiscrepancyReportService {

    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final ConditionDefinitionRepository conditionDefinitionRepository;
    private final AllocationRepository allocationRepository;

    @Override
    public DiscrepancyReportResponseDto create(DiscrepancyReportRequestDto request) {
        ConditionDefinition condition = resolveCondition(request.getConditionDefinitionId());
        DiscrepancyReport entity = DiscrepancyReport.builder()
                .createdFrom(request.getCreatedFrom())
                .refId(request.getRefId())
                .discrepancyType(request.getDiscrepancyType())
                .conditionDefinition(condition)
                .allocation(resolveAllocation(request.getOrderDetailId(), request.getDeviceId()))
                .penaltyAmount(resolvePenalty(condition))
                .staffNote(request.getStaffNote())
                .build();
        return DiscrepancyReportResponseDto.from(discrepancyReportRepository.save(entity));
    }

    @Override
    public DiscrepancyReportResponseDto update(Long id, DiscrepancyReportRequestDto request) {
        DiscrepancyReport entity = discrepancyReportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy discrepancy report: " + id));
        entity.setCreatedFrom(request.getCreatedFrom());
        entity.setRefId(request.getRefId());
        ConditionDefinition condition = resolveCondition(request.getConditionDefinitionId());
        entity.setDiscrepancyType(request.getDiscrepancyType());
        entity.setConditionDefinition(condition);
        entity.setAllocation(resolveAllocation(request.getOrderDetailId(), request.getDeviceId()));
        entity.setPenaltyAmount(resolvePenalty(condition));
        entity.setStaffNote(request.getStaffNote());
        return DiscrepancyReportResponseDto.from(discrepancyReportRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public DiscrepancyReportResponseDto getById(Long id) {
        DiscrepancyReport entity = discrepancyReportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy discrepancy report: " + id));
        return DiscrepancyReportResponseDto.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscrepancyReportResponseDto> getByReference(DiscrepancyCreatedFrom createdFrom, Long refId) {
        return discrepancyReportRepository.findByCreatedFromAndRefIdOrderByCreatedAtDesc(createdFrom, refId)
                .stream()
                .map(DiscrepancyReportResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscrepancyReportResponseDto> getAll() {
        return discrepancyReportRepository.findAll().stream()
                .map(DiscrepancyReportResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscrepancyReportResponseDto> getByOrderId(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        return discrepancyReportRepository.findByAllocation_OrderDetail_RentalOrder_OrderId(orderId)
                .stream()
                .map(DiscrepancyReportResponseDto::from)
                .toList();
    }

    private ConditionDefinition resolveCondition(Long conditionDefinitionId) {
        if (conditionDefinitionId == null) {
            return null;
        }
        return conditionDefinitionRepository.findById(conditionDefinitionId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy định nghĩa tình trạng với mã: " + conditionDefinitionId));
    }

    private Allocation resolveAllocation(Long orderDetailId, Long deviceId) {
        if (orderDetailId == null || deviceId == null) {
            throw new IllegalArgumentException("Cần cung cấp mã chi tiết đơn thuê và mã thiết bị để xác định phân bổ");
        }
        return allocationRepository.findByOrderDetail_OrderDetailIdAndDevice_DeviceId(orderDetailId, deviceId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy phân bổ cho chi tiết đơn thuê " + orderDetailId + " và thiết bị " + deviceId));
    }

    private BigDecimal resolvePenalty(ConditionDefinition conditionDefinition) {
        if (conditionDefinition == null || conditionDefinition.getDefaultCompensation() == null) {
            return BigDecimal.ZERO;
        }
        return conditionDefinition.getDefaultCompensation();
    }

}
