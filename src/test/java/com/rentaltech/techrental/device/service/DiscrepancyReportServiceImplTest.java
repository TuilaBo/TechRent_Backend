package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.*;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportRequestDto;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.ConditionDefinitionRepository;
import com.rentaltech.techrental.device.repository.DiscrepancyReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscrepancyReportServiceImplTest {

    @Mock
    private DiscrepancyReportRepository discrepancyReportRepository;
    @Mock
    private ConditionDefinitionRepository conditionDefinitionRepository;
    @Mock
    private AllocationRepository allocationRepository;

    @InjectMocks
    private DiscrepancyReportServiceImpl service;

    @Test
    void createUsesDefaultCompensation() {
        DiscrepancyReportRequestDto request = baseRequestBuilder().conditionDefinitionId(3L).build();
        ConditionDefinition definition = ConditionDefinition.builder()
                .conditionDefinitionId(3L)
                .defaultCompensation(BigDecimal.TEN)
                .build();
        when(conditionDefinitionRepository.findById(3L)).thenReturn(Optional.of(definition));
        Allocation allocation = Allocation.builder().allocationId(9L).build();
        when(allocationRepository.findByOrderDetail_OrderDetailIdAndDevice_DeviceId(4L, 5L)).thenReturn(Optional.of(allocation));
        when(discrepancyReportRepository.save(any(DiscrepancyReport.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(request);

        ArgumentCaptor<DiscrepancyReport> captor = ArgumentCaptor.forClass(DiscrepancyReport.class);
        verify(discrepancyReportRepository).save(captor.capture());
        assertThat(captor.getValue().getPenaltyAmount()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void createWithNullConditionDefinitionSetsZeroPenalty() {
        DiscrepancyReportRequestDto request = baseRequestBuilder().conditionDefinitionId(null).build();
        Allocation allocation = Allocation.builder().allocationId(9L).build();
        when(allocationRepository.findByOrderDetail_OrderDetailIdAndDevice_DeviceId(4L, 5L)).thenReturn(Optional.of(allocation));
        when(discrepancyReportRepository.save(any(DiscrepancyReport.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(request);

        verify(conditionDefinitionRepository, never()).findById(any());
    }

    @Test
    void createRequiresAllocationIdentifiers() {
        DiscrepancyReportRequestDto request = baseRequestBuilder().deviceId(null).build();
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DiscrepancyReportRequestDto.DiscrepancyReportRequestDtoBuilder baseRequestBuilder() {
        return DiscrepancyReportRequestDto.builder()
                .createdFrom(DiscrepancyCreatedFrom.QC_REPORT)
                .refId(1L)
                .discrepancyType(DiscrepancyType.DAMAGE)
                .orderDetailId(4L)
                .deviceId(5L)
                .staffNote("note");
    }
}
