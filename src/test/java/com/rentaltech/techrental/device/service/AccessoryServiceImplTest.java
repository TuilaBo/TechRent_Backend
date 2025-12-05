package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Accessory;
import com.rentaltech.techrental.device.model.AccessoryCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.dto.AccessoryRequestDto;
import com.rentaltech.techrental.device.model.dto.AccessoryResponseDto;
import com.rentaltech.techrental.device.repository.AccessoryCategoryRepository;
import com.rentaltech.techrental.device.repository.AccessoryRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessoryServiceImplTest {

    @Mock
    private AccessoryRepository accessoryRepository;
    @Mock
    private AccessoryCategoryRepository accessoryCategoryRepository;
    @Mock
    private DeviceModelRepository deviceModelRepository;

    @InjectMocks
    private AccessoryServiceImpl service;

    private AccessoryCategory category;
    private DeviceModel deviceModel;

    @BeforeEach
    void setUp() {
        category = AccessoryCategory.builder()
                .accessoryCategoryId(1L)
                .accessoryCategoryName("Cables")
                .build();
        deviceModel = DeviceModel.builder()
                .deviceModelId(2L)
                .deviceName("Model X")
                .build();
    }

    @Test
    void createRequiresCategoryId() {
        AccessoryRequestDto request = AccessoryRequestDto.builder()
                .accessoryName("HDMI cable")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accessoryCategoryId");
    }

    @Test
    void createPersistsAccessoryWithResolvedRelations() {
        AccessoryRequestDto request = AccessoryRequestDto.builder()
                .accessoryName("HDMI cable")
                .description("Desc")
                .imageUrl("img.png")
                .isActive(true)
                .accessoryCategoryId(1L)
                .deviceModelId(2L)
                .build();

        when(accessoryRepository.save(any(Accessory.class))).thenAnswer(inv -> {
            Accessory accessory = inv.getArgument(0);
            accessory.setAccessoryId(99L);
            return accessory;
        });
        when(accessoryCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(deviceModelRepository.findById(2L)).thenReturn(Optional.of(deviceModel));

        AccessoryResponseDto response = service.create(request);

        assertThat(response.getAccessoryId()).isEqualTo(99L);
        ArgumentCaptor<Accessory> captor = ArgumentCaptor.forClass(Accessory.class);
        verify(accessoryRepository).save(captor.capture());
        Accessory saved = captor.getValue();
        assertThat(saved.getAccessoryCategory()).isEqualTo(category);
        assertThat(saved.getDeviceModel()).isEqualTo(deviceModel);
    }

    @Test
    void updateRefreshesCategoryAndModel() {
        Accessory existing = Accessory.builder()
                .accessoryId(10L)
                .accessoryName("Old")
                .accessoryCategory(category)
                .build();
        when(accessoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(accessoryCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(deviceModelRepository.findById(2L)).thenReturn(Optional.of(deviceModel));
        when(accessoryRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));
        AccessoryRequestDto request = AccessoryRequestDto.builder()
                .accessoryName("New")
                .description("Updated")
                .imageUrl("new.png")
                .isActive(true)
                .accessoryCategoryId(1L)
                .deviceModelId(2L)
                .build();

        AccessoryResponseDto response = service.update(10L, request);

        assertThat(response.getAccessoryName()).isEqualTo("New");
        verify(accessoryRepository).save(existing);
    }
}
