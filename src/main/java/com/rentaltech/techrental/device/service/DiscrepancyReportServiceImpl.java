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
import java.util.Optional;

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
                .customerNote(request.getCustomerNote())
                .build();
        return mapToDto(discrepancyReportRepository.save(entity));
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
        entity.setCustomerNote(request.getCustomerNote());
        return mapToDto(discrepancyReportRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public DiscrepancyReportResponseDto getById(Long id) {
        DiscrepancyReport entity = discrepancyReportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy discrepancy report: " + id));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscrepancyReportResponseDto> getByReference(DiscrepancyCreatedFrom createdFrom, Long refId) {
        return discrepancyReportRepository.findByCreatedFromAndRefIdOrderByCreatedAtDesc(createdFrom, refId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscrepancyReportResponseDto> getAll() {
        return discrepancyReportRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    private ConditionDefinition resolveCondition(Long conditionDefinitionId) {
        if (conditionDefinitionId == null) {
            return null;
        }
        return conditionDefinitionRepository.findById(conditionDefinitionId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy condition definition: " + conditionDefinitionId));
    }

    private Allocation resolveAllocation(Long orderDetailId, Long deviceId) {
        if (orderDetailId == null || deviceId == null) {
            throw new IllegalArgumentException("Cần cung cấp orderDetailId và deviceId để xác định allocation");
        }
        return allocationRepository.findByOrderDetail_OrderDetailIdAndDevice_DeviceId(orderDetailId, deviceId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy allocation cho orderDetail " + orderDetailId + " và device " + deviceId));
    }

    private BigDecimal resolvePenalty(ConditionDefinition conditionDefinition) {
        if (conditionDefinition == null || conditionDefinition.getDefaultCompensation() == null) {
            return BigDecimal.ZERO;
        }
        return conditionDefinition.getDefaultCompensation();
    }

    private DiscrepancyReportResponseDto mapToDto(DiscrepancyReport entity) {
        ConditionDefinition definition = entity.getConditionDefinition();
        return DiscrepancyReportResponseDto.builder()
                .discrepancyReportId(entity.getDiscrepancyReportId())
                .createdFrom(entity.getCreatedFrom())
                .refId(entity.getRefId())
                .discrepancyType(entity.getDiscrepancyType())
                .conditionDefinitionId(Optional.ofNullable(definition).map(ConditionDefinition::getConditionDefinitionId).orElse(null))
                .conditionName(Optional.ofNullable(definition).map(ConditionDefinition::getName).orElse(null))
                .allocationId(Optional.ofNullable(entity.getAllocation()).map(Allocation::getAllocationId).orElse(null))
                .penaltyAmount(entity.getPenaltyAmount())
                .staffNote(entity.getStaffNote())
                .customerNote(entity.getCustomerNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
