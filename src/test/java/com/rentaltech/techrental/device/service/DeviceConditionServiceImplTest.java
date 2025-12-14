package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.*;
import com.rentaltech.techrental.device.model.dto.DeviceConditionRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceConditionResponseDto;
import com.rentaltech.techrental.device.repository.ConditionDefinitionRepository;
import com.rentaltech.techrental.device.repository.DeviceConditionRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceConditionServiceImplTest {

    @Mock
    private DeviceConditionRepository deviceConditionRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private ConditionDefinitionRepository conditionDefinitionRepository;
    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private DeviceConditionServiceImpl service;

    @Test
    void getByDeviceIdsGroupsByDevice() {
        DeviceCondition first = DeviceCondition.builder()
                .device(Device.builder().deviceId(1L).build())
                .conditionDefinition(ConditionDefinition.builder().conditionDefinitionId(5L).name("A").build())
                .build();
        DeviceCondition second = DeviceCondition.builder()
                .device(Device.builder().deviceId(2L).build())
                .conditionDefinition(ConditionDefinition.builder().conditionDefinitionId(6L).name("B").build())
                .build();
        when(deviceConditionRepository.findByDevice_DeviceIdIn(Set.of(1L, 2L)))
                .thenReturn(List.of(first, second));

        Map<Long, List<DeviceConditionResponseDto>> map = service.getByDeviceIds(Set.of(1L, 2L));

        assertThat(map).hasSize(2);
        assertThat(map.get(1L)).hasSize(1);
    }

    @Test
    void upsertConditionsDeletesExistingAndUpdatesStatus() {
        Device device = Device.builder().deviceId(10L).status(DeviceStatus.AVAILABLE).build();
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        Staff staff = Staff.builder().staffId(4L).build();
        when(staffRepository.findByAccount_Username("tech")).thenReturn(Optional.of(staff));
        ConditionDefinition definition = ConditionDefinition.builder()
                .conditionDefinitionId(3L)
                .conditionType(ConditionType.DAMAGED)
                .conditionSeverity(ConditionSeverity.HIGH)
                .build();
        when(conditionDefinitionRepository.findAllById(Set.of(3L))).thenReturn(List.of(definition));
        when(deviceConditionRepository.save(any(DeviceCondition.class))).thenAnswer(inv -> inv.getArgument(0));

        List<DeviceConditionResponseDto> responses = service.upsertConditions(
                10L,
                List.of(DeviceConditionRequestDto.builder().conditionDefinitionId(3L).note("note").build()),
                "tech");

        assertThat(responses).hasSize(1);
        verify(deviceConditionRepository).deleteByDevice_DeviceId(10L);
        verify(deviceRepository).save(device);
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.DAMAGED);
    }

    @Test
    void upsertConditionsRejectsMixedTypes() {
        Device device = Device.builder().deviceId(10L).build();
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(staffRepository.findByAccount_Username("tech")).thenReturn(Optional.of(Staff.builder().staffId(1L).build()));
        ConditionDefinition def1 = ConditionDefinition.builder()
                .conditionDefinitionId(3L)
                .conditionType(ConditionType.DAMAGED)
                .build();
        ConditionDefinition def2 = ConditionDefinition.builder()
                .conditionDefinitionId(4L)
                .conditionType(ConditionType.GOOD)
                .build();
        when(conditionDefinitionRepository.findAllById(Set.of(3L, 4L))).thenReturn(List.of(def1, def2));

        assertThatThrownBy(() -> service.upsertConditions(10L,
                        List.of(
                                DeviceConditionRequestDto.builder().conditionDefinitionId(3L).build(),
                                DeviceConditionRequestDto.builder().conditionDefinitionId(4L).build()
                        ),
                        "tech"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upsertConditionsReturnsExistingWhenEmptyInput() {
        Device device = Device.builder().deviceId(30L).build();
        when(deviceRepository.findById(30L)).thenReturn(Optional.of(device));
        when(deviceConditionRepository.findByDevice_DeviceId(30L)).thenReturn(List.of());

        List<DeviceConditionResponseDto> responses = service.upsertConditions(30L, List.of(), "user");

        assertThat(responses).isEmpty();
        verify(deviceConditionRepository, never()).deleteByDevice_DeviceId(any());
    }

    @Test
    void upsertConditionsAllowsNullUsername() {
        Device device = Device.builder().deviceId(40L).status(DeviceStatus.AVAILABLE).build();
        when(deviceRepository.findById(40L)).thenReturn(Optional.of(device));
        ConditionDefinition def = ConditionDefinition.builder()
                .conditionDefinitionId(8L)
                .conditionType(ConditionType.GOOD)
                .conditionSeverity(ConditionSeverity.INFO)
                .build();
        when(conditionDefinitionRepository.findAllById(Set.of(8L))).thenReturn(List.of(def));
        when(deviceConditionRepository.save(any(DeviceCondition.class))).thenAnswer(inv -> inv.getArgument(0));

        List<DeviceConditionResponseDto> responses = service.upsertConditions(
                40L,
                List.of(DeviceConditionRequestDto.builder().conditionDefinitionId(8L).build()),
                null);

        assertThat(responses).hasSize(1);
        // Status should be set to AVAILABLE for GOOD condition
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.AVAILABLE);
    }

    @Test
    void upsertConditionsThrowsWhenDefinitionMissing() {
        Device device = Device.builder().deviceId(50L).build();
        when(deviceRepository.findById(50L)).thenReturn(Optional.of(device));
        when(staffRepository.findByAccount_Username("tech")).thenReturn(Optional.of(Staff.builder().staffId(1L).build()));
        when(conditionDefinitionRepository.findAllById(Set.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.upsertConditions(50L,
                List.of(DeviceConditionRequestDto.builder().conditionDefinitionId(99L).build()),
                "tech"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateConditionChangesDeviceStatus() {
        Device device = Device.builder().deviceId(20L).status(DeviceStatus.AVAILABLE).build();
        when(deviceRepository.findById(20L)).thenReturn(Optional.of(device));
        ConditionDefinition definition = ConditionDefinition.builder()
                .conditionDefinitionId(7L)
                .conditionType(ConditionType.LOST)
                .conditionSeverity(ConditionSeverity.CRITICAL)
                .build();
        when(conditionDefinitionRepository.findById(7L)).thenReturn(Optional.of(definition));
        when(staffRepository.findById(2L)).thenReturn(Optional.of(Staff.builder().staffId(2L).build()));
        when(deviceConditionRepository.save(any(DeviceCondition.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateCondition(20L, 7L, null, null, null, 2L);

        verify(deviceRepository).save(device);
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.LOST);
    }

    @Test
    void updateConditionNoopsWhenIdsMissing() {
        service.updateCondition(null, null, null, null, null, null);
        verify(deviceRepository, never()).findById(any());
    }

    @Test
    void updateConditionDefaultsSeverityForGoodAndSetsAvailable() {
        Device device = Device.builder().deviceId(30L).status(DeviceStatus.DAMAGED).build();
        when(deviceRepository.findById(30L)).thenReturn(Optional.of(device));
        ConditionDefinition definition = ConditionDefinition.builder()
                .conditionDefinitionId(11L)
                .conditionType(ConditionType.GOOD)
                .build();
        when(conditionDefinitionRepository.findById(11L)).thenReturn(Optional.of(definition));
        when(staffRepository.findById(2L)).thenReturn(Optional.of(Staff.builder().staffId(2L).build()));
        when(deviceConditionRepository.save(any(DeviceCondition.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateCondition(30L, 11L, null, null, null, 2L);

        ArgumentCaptor<DeviceCondition> captor = ArgumentCaptor.forClass(DeviceCondition.class);
        verify(deviceConditionRepository).save(captor.capture());
        DeviceCondition saved = captor.getValue();
        assertThat(saved.getSeverity()).isEqualTo(ConditionSeverity.INFO.name());
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.AVAILABLE);
    }

    @Test
    void updateConditionUsesProvidedSeverityWhenDamaged() {
        Device device = Device.builder().deviceId(31L).status(DeviceStatus.AVAILABLE).build();
        when(deviceRepository.findById(31L)).thenReturn(Optional.of(device));
        ConditionDefinition definition = ConditionDefinition.builder()
                .conditionDefinitionId(12L)
                .conditionType(ConditionType.DAMAGED)
                .build();
        when(conditionDefinitionRepository.findById(12L)).thenReturn(Optional.of(definition));
        when(staffRepository.findById(1L)).thenReturn(Optional.of(Staff.builder().staffId(1L).build()));
        when(deviceConditionRepository.save(any(DeviceCondition.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateCondition(31L, 12L, ConditionSeverity.HIGH.name(), "note", null, 1L);

        ArgumentCaptor<DeviceCondition> captor = ArgumentCaptor.forClass(DeviceCondition.class);
        verify(deviceConditionRepository).save(captor.capture());
        DeviceCondition saved = captor.getValue();
        assertThat(saved.getSeverity()).isEqualTo(ConditionSeverity.HIGH.name());
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.DAMAGED);
    }

    @Test
    void updateConditionThrowsWhenDefinitionHasNullType() {
        Device device = Device.builder().deviceId(32L).status(DeviceStatus.AVAILABLE).build();
        when(deviceRepository.findById(32L)).thenReturn(Optional.of(device));
        ConditionDefinition definition = ConditionDefinition.builder()
                .conditionDefinitionId(13L)
                .conditionType(null)
                .build();
        when(conditionDefinitionRepository.findById(13L)).thenReturn(Optional.of(definition));

        assertThatThrownBy(() -> service.updateCondition(32L, 13L, null, null, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateConditionCapturesStaffWhenProvided() {
        Device device = Device.builder().deviceId(33L).status(DeviceStatus.AVAILABLE).build();
        when(deviceRepository.findById(33L)).thenReturn(Optional.of(device));
        ConditionDefinition definition = ConditionDefinition.builder()
                .conditionDefinitionId(14L)
                .conditionType(ConditionType.GOOD)
                .build();
        when(conditionDefinitionRepository.findById(14L)).thenReturn(Optional.of(definition));
        Staff staff = Staff.builder().staffId(5L).build();
        when(staffRepository.findById(5L)).thenReturn(Optional.of(staff));
        when(deviceConditionRepository.save(any(DeviceCondition.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateCondition(33L, 14L, null, null, null, 5L);

        ArgumentCaptor<DeviceCondition> captor = ArgumentCaptor.forClass(DeviceCondition.class);
        verify(deviceConditionRepository).save(captor.capture());
        DeviceCondition saved = captor.getValue();
        assertThat(saved.getCapturedBy()).isEqualTo(staff);
    }
}
