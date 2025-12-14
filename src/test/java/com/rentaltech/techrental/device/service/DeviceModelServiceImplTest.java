package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Brand;
import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.dto.DeviceModelRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceModelResponseDto;
import com.rentaltech.techrental.device.repository.BrandRepository;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceModelServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceModelRepository deviceModelRepository;
    @Mock
    private DeviceCategoryRepository deviceCategoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private DeviceModelServiceImpl service;

    private DeviceModelRequestDto baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = DeviceModelRequestDto.builder()
                .deviceName("Laptop X")
                .brandId(1L)
                .deviceCategoryId(2L)
                .description("High-end")
                .specifications("Specs")
                .deviceValue(BigDecimal.valueOf(1000))
                .pricePerDay(BigDecimal.valueOf(100))
                .depositPercent(BigDecimal.valueOf(10))
                .isActive(true)
                .build();
    }

    @Test
    void createUploadsImageWhenProvided() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(imageStorageService.uploadDeviceModelImage(file, 1L, "Laptop X")).thenReturn("https://img");
        when(deviceModelRepository.save(any(DeviceModel.class))).thenAnswer(inv -> {
            DeviceModel model = inv.getArgument(0);
            model.setDeviceModelId(5L);
            return model;
        });
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.builder().brandId(1L).brandName("Brand").build()));
        when(deviceCategoryRepository.findById(2L)).thenReturn(Optional.of(DeviceCategory.builder().deviceCategoryId(2L).deviceCategoryName("Cat").build()));

        DeviceModelResponseDto response = service.create(baseRequest, file);

        assertThat(response.getDeviceModelId()).isEqualTo(5L);
        verify(imageStorageService).uploadDeviceModelImage(file, 1L, "Laptop X");
    }

    @Test
    void createSkipsUploadWhenImageNull() {
        when(deviceModelRepository.save(any(DeviceModel.class))).thenAnswer(inv -> {
            DeviceModel model = inv.getArgument(0);
            model.setDeviceModelId(6L);
            return model;
        });
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.builder().brandId(1L).brandName("Brand").build()));
        when(deviceCategoryRepository.findById(2L)).thenReturn(Optional.of(DeviceCategory.builder().deviceCategoryId(2L).deviceCategoryName("Cat").build()));

        DeviceModelResponseDto response = service.create(baseRequest, null);

        assertThat(response.getDeviceModelId()).isEqualTo(6L);
        verify(imageStorageService, never()).uploadDeviceModelImage(any(), any(), any());
    }

    @Test
    void createSkipsUploadWhenImageEmpty() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        when(deviceModelRepository.save(any(DeviceModel.class))).thenAnswer(inv -> {
            DeviceModel model = inv.getArgument(0);
            model.setDeviceModelId(7L);
            return model;
        });
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.builder().brandId(1L).brandName("Brand").build()));
        when(deviceCategoryRepository.findById(2L)).thenReturn(Optional.of(DeviceCategory.builder().deviceCategoryId(2L).deviceCategoryName("Cat").build()));

        DeviceModelResponseDto response = service.create(baseRequest, file);

        assertThat(response.getDeviceModelId()).isEqualTo(7L);
        verify(imageStorageService, never()).uploadDeviceModelImage(any(), any(), any());
    }

    @Test
    void createThrowsWhenBrandIdNull() {
        DeviceModelRequestDto req = DeviceModelRequestDto.builder()
                .deviceName("Laptop X")
                .brandId(null)
                .deviceCategoryId(2L)
                .description("High-end")
                .specifications("Specs")
                .deviceValue(java.math.BigDecimal.valueOf(1000))
                .pricePerDay(java.math.BigDecimal.valueOf(100))
                .depositPercent(java.math.BigDecimal.valueOf(10))
                .isActive(true)
                .build();

        assertThatThrownBy(() -> service.create(req, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createThrowsWhenCategoryIdNull() {
        DeviceModelRequestDto req = DeviceModelRequestDto.builder()
                .deviceName("Laptop X")
                .brandId(1L)
                .deviceCategoryId(null)
                .description("High-end")
                .specifications("Specs")
                .deviceValue(java.math.BigDecimal.valueOf(1000))
                .pricePerDay(java.math.BigDecimal.valueOf(100))
                .depositPercent(java.math.BigDecimal.valueOf(10))
                .isActive(true)
                .build();

        assertThatThrownBy(() -> service.create(req, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findByDeviceCategoryThrowsWhenMissing() {
        when(deviceCategoryRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByDeviceCategory(2L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateRecalculatesAvailability() {
        DeviceModel existing = DeviceModel.builder()
                .deviceModelId(7L)
                .amountAvailable(1L)
                .build();
        when(deviceModelRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(deviceModelRepository.save(existing)).thenAnswer(inv -> existing);
        when(deviceRepository.countByDeviceModel_DeviceModelId(7L)).thenReturn(4L);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.builder().brandId(1L).brandName("Brand").build()));
        when(deviceCategoryRepository.findById(2L)).thenReturn(Optional.of(DeviceCategory.builder().deviceCategoryId(2L).deviceCategoryName("Cat").build()));

        DeviceModelResponseDto response = service.update(7L, baseRequest, null);

        assertThat(response.getDeviceModelId()).isEqualTo(7L);
        assertThat(existing.getAmountAvailable()).isEqualTo(4L);
        verify(imageStorageService, never()).uploadDeviceModelImage(any(), any(), any());
    }

    @Test
    void updateUploadsImageWhenProvided() {
        DeviceModel existing = DeviceModel.builder()
                .deviceModelId(8L)
                .amountAvailable(0L)
                .build();
        when(deviceModelRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(deviceRepository.countByDeviceModel_DeviceModelId(8L)).thenReturn(0L);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(Brand.builder().brandId(1L).brandName("Brand").build()));
        when(deviceCategoryRepository.findById(2L)).thenReturn(Optional.of(DeviceCategory.builder().deviceCategoryId(2L).deviceCategoryName("Cat").build()));
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(imageStorageService.uploadDeviceModelImage(file, 1L, "Laptop X")).thenReturn("https://img");
        when(deviceModelRepository.save(any(DeviceModel.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceModelResponseDto response = service.update(8L, baseRequest, file);

        assertThat(response.getDeviceModelId()).isEqualTo(8L);
        verify(imageStorageService).uploadDeviceModelImage(file, 1L, "Laptop X");
    }

    @Test
    void updateThrowsWhenBrandIdNull() {
        DeviceModel existing = DeviceModel.builder().deviceModelId(9L).build();
        when(deviceModelRepository.findById(9L)).thenReturn(Optional.of(existing));

        DeviceModelRequestDto req = DeviceModelRequestDto.builder()
                .deviceName("Model")
                .brandId(null)
                .deviceCategoryId(2L)
                .isActive(true)
                .build();

        assertThatThrownBy(() -> service.update(9L, req, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateThrowsWhenCategoryIdNull() {
        DeviceModel existing = DeviceModel.builder().deviceModelId(10L).build();
        when(deviceModelRepository.findById(10L)).thenReturn(Optional.of(existing));

        DeviceModelRequestDto req = DeviceModelRequestDto.builder()
                .deviceName("Model")
                .brandId(1L)
                .deviceCategoryId(null)
                .isActive(true)
                .build();

        assertThatThrownBy(() -> service.update(10L, req, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchDelegatesToRepositoryQuery() {
        Pageable pageable = Pageable.unpaged();
        when(deviceModelRepository.searchDeviceModels(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<DeviceModelResponseDto> result = service.search(null, null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isZero();
        verify(deviceModelRepository).searchDeviceModels(
                any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }
}
