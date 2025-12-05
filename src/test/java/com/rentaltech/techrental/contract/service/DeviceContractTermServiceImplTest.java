package com.rentaltech.techrental.contract.service;

import com.rentaltech.techrental.contract.model.DeviceContractTerm;
import com.rentaltech.techrental.contract.model.dto.DeviceContractTermRequestDto;
import com.rentaltech.techrental.contract.model.dto.DeviceContractTermResponseDto;
import com.rentaltech.techrental.contract.repository.DeviceContractTermRepository;
import com.rentaltech.techrental.contract.service.impl.DeviceContractTermServiceImpl;
import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceContractTermServiceImplTest {

    @InjectMocks
    private DeviceContractTermServiceImpl service;

    @Mock
    private DeviceContractTermRepository termRepository;
    @Mock
    private DeviceModelRepository deviceModelRepository;
    @Mock
    private DeviceCategoryRepository deviceCategoryRepository;

    private DeviceModel model;
    private DeviceCategory category;

    @BeforeEach
    void setUp() {
        category = DeviceCategory.builder().deviceCategoryId(2L).deviceCategoryName("Laptop").build();
        model = DeviceModel.builder()
                .deviceModelId(1L)
                .deviceName("Model A")
                .deviceCategory(category)
                .build();
    }

    @Test
    void createPersistsNewTerm() {
        DeviceContractTermRequestDto request = buildRequestWithModel();
        when(deviceModelRepository.findById(model.getDeviceModelId())).thenReturn(Optional.of(model));
        DeviceContractTerm saved = DeviceContractTerm.builder()
                .deviceContractTermId(10L)
                .title(request.getTitle())
                .content(request.getContent())
                .deviceModel(model)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(termRepository.save(any(DeviceContractTerm.class))).thenReturn(saved);

        DeviceContractTermResponseDto response = service.create(request, 99L);

        assertThat(response.getDeviceContractTermId()).isEqualTo(10L);
        verify(termRepository).save(any(DeviceContractTerm.class));
    }

    @Test
    void updateEditsExistingTerm() {
        DeviceContractTerm existing = DeviceContractTerm.builder()
                .deviceContractTermId(10L)
                .title("Old")
                .content("Old content")
                .deviceModel(model)
                .active(true)
                .build();
        when(termRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(deviceModelRepository.findById(model.getDeviceModelId())).thenReturn(Optional.of(model));
        when(termRepository.save(existing)).thenReturn(existing);
        DeviceContractTermRequestDto request = buildRequestWithModel();
        request.setTitle("New");
        request.setContent("New content");

        DeviceContractTermResponseDto response = service.update(10L, request, 77L);

        assertThat(response.getTitle()).isEqualTo("New");
        assertThat(existing.getUpdatedBy()).isEqualTo(77L);
    }

    @Test
    void deleteRemovesTerm() {
        DeviceContractTerm existing = DeviceContractTerm.builder()
                .deviceContractTermId(10L)
                .build();
        when(termRepository.findById(10L)).thenReturn(Optional.of(existing));

        service.delete(10L);

        verify(termRepository).delete(existing);
    }

    @Test
    void getReturnsDto() {
        DeviceContractTerm term = DeviceContractTerm.builder()
                .deviceContractTermId(15L)
                .title("Title")
                .content("Content")
                .build();
        when(termRepository.findById(15L)).thenReturn(Optional.of(term));

        DeviceContractTermResponseDto response = service.get(15L);

        assertThat(response.getDeviceContractTermId()).isEqualTo(15L);
    }

    @Test
    void listSupportsFilteringByModelAndActiveFlag() {
        DeviceContractTerm term = DeviceContractTerm.builder()
                .deviceContractTermId(1L)
                .deviceModel(model)
                .active(true)
                .build();
        when(termRepository.findByDeviceModel_DeviceModelId(model.getDeviceModelId())).thenReturn(List.of(term));

        List<DeviceContractTermResponseDto> result = service.list(model.getDeviceModelId(), null, true);

        assertThat(result).hasSize(1);
    }

    @Test
    void listFallsBackToAllWhenNoFilters() {
        DeviceContractTerm inactive = DeviceContractTerm.builder()
                .deviceContractTermId(2L)
                .active(false)
                .build();
        when(termRepository.findAll()).thenReturn(List.of(inactive));

        List<DeviceContractTermResponseDto> result = service.list(null, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findApplicableTermsCollectsByModelAndCategoryAndAddsDefault() {
        RentalOrder order = RentalOrder.builder().orderId(1L).build();
        OrderDetail detail = OrderDetail.builder().deviceModel(model).build();
        when(termRepository.findByDeviceModel_DeviceModelIdInAndActiveIsTrue(anyCollection()))
                .thenReturn(List.of(DeviceContractTerm.builder().deviceContractTermId(1L).deviceModel(model).build()));
        when(termRepository.findByDeviceCategory_DeviceCategoryIdInAndActiveIsTrue(anyCollection()))
                .thenReturn(List.of(DeviceContractTerm.builder().deviceContractTermId(2L).deviceCategory(category).build()));
        when(termRepository.findByDeviceModelIsNullAndDeviceCategoryIsNullAndActiveIsTrue())
                .thenReturn(List.of(DeviceContractTerm.builder().deviceContractTermId(3L).build()));

        List<DeviceContractTerm> result = service.findApplicableTerms(order, List.of(detail));

        assertThat(result).hasSize(3);
    }

    @Test
    void validateScopeRequiresSingleScope() {
        DeviceContractTermRequestDto invalid = new DeviceContractTermRequestDto();
        invalid.setTitle("title");
        invalid.setContent("content");

        assertThatThrownBy(() -> service.create(invalid, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        invalid.setDeviceModelId(1L);
        invalid.setDeviceCategoryId(2L);
        assertThatThrownBy(() -> service.create(invalid, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DeviceContractTermRequestDto buildRequestWithModel() {
        DeviceContractTermRequestDto dto = new DeviceContractTermRequestDto();
        dto.setTitle("Bảo hành");
        dto.setContent("Nội dung");
        dto.setDeviceModelId(model.getDeviceModelId());
        dto.setActive(true);
        return dto;
    }
}
