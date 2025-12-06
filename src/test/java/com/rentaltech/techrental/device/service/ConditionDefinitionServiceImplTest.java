package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.ConditionDefinition;
import com.rentaltech.techrental.device.model.ConditionSeverity;
import com.rentaltech.techrental.device.model.ConditionType;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.dto.ConditionDefinitionRequestDto;
import com.rentaltech.techrental.device.model.dto.ConditionDefinitionResponseDto;
import com.rentaltech.techrental.device.repository.ConditionDefinitionRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionDefinitionServiceImplTest {

    @Mock
    private ConditionDefinitionRepository conditionDefinitionRepository;
    @Mock
    private DeviceModelRepository deviceModelRepository;

    @InjectMocks
    private ConditionDefinitionServiceImpl service;

    private ConditionDefinitionRequestDto request;

    @BeforeEach
    void setUp() {
        request = ConditionDefinitionRequestDto.builder()
                .name("Scratch")
                .deviceModelId(1L)
                .description("Minor scratch")
                .conditionType(ConditionType.DAMAGED)
                .conditionSeverity(ConditionSeverity.HIGH)
                .defaultCompensation(BigDecimal.TEN)
                .build();
    }

    @Test
    void createRequiresDeviceModel() {
        when(deviceModelRepository.findById(1L)).thenReturn(Optional.of(DeviceModel.builder().deviceModelId(1L).build()));
        when(conditionDefinitionRepository.save(any(ConditionDefinition.class)))
                .thenAnswer(inv -> {
                    ConditionDefinition def = inv.getArgument(0);
                    def.setConditionDefinitionId(5L);
                    return def;
                });
        ConditionDefinitionResponseDto response = service.create(request);
        assertThat(response.getConditionDefinitionId()).isEqualTo(5L);
    }

    @Test
    void createThrowsWhenDeviceModelMissing() {
        request.setDeviceModelId(null);
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
