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
    void createWithCategoryPersistsNewTerm() {
        DeviceContractTermRequestDto request = new DeviceContractTermRequestDto();
        request.setTitle("Warranty");
        request.setContent("Content");
        request.setDeviceCategoryId(category.getDeviceCategoryId());
        request.setActive(true);

        when(deviceCategoryRepository.findById(category.getDeviceCategoryId())).thenReturn(Optional.of(category));
        DeviceContractTerm saved = DeviceContractTerm.builder()
                .deviceContractTermId(11L)
                .title(request.getTitle())
                .content(request.getContent())
                .deviceCategory(category)
                .active(true)
                .build();
        when(termRepository.save(any(DeviceContractTerm.class))).thenReturn(saved);

        DeviceContractTermResponseDto response = service.create(request, 42L);

        assertThat(response.getDeviceContractTermId()).isEqualTo(11L);
        verify(termRepository).save(any(DeviceContractTerm.class));
    }

    @Test
    void createThrowsWhenModelNotFound() {
        DeviceContractTermRequestDto request = new DeviceContractTermRequestDto();
        request.setTitle("Warranty");
        request.setContent("Content");
        request.setDeviceModelId(999L); // non-existing
        request.setActive(true);

        when(deviceModelRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy model thiết bị");
    }

    @Test
    void createThrowsWhenCategoryNotFound() {
        DeviceContractTermRequestDto request = new DeviceContractTermRequestDto();
        request.setTitle("Warranty");
        request.setContent("Content");
        request.setDeviceCategoryId(888L); // non-existing
        request.setActive(true);

        when(deviceCategoryRepository.findById(888L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy loại thiết bị");
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
    void updateWithCategoryEditsExistingTerm() {
        DeviceContractTerm existing = DeviceContractTerm.builder()
                .deviceContractTermId(20L)
                .title("Old")
                .content("Old content")
                .active(true)
                .build();
        when(termRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(deviceCategoryRepository.findById(category.getDeviceCategoryId())).thenReturn(Optional.of(category));
        when(termRepository.save(existing)).thenReturn(existing);

        DeviceContractTermRequestDto request = new DeviceContractTermRequestDto();
        request.setTitle("NewCat");
        request.setContent("New content cat");
        request.setDeviceCategoryId(category.getDeviceCategoryId());
        request.setActive(true);

        DeviceContractTermResponseDto response = service.update(20L, request, 88L);

        assertThat(response.getTitle()).isEqualTo("NewCat");
        assertThat(existing.getDeviceCategory().getDeviceCategoryId()).isEqualTo(category.getDeviceCategoryId());
        assertThat(existing.getUpdatedBy()).isEqualTo(88L);
    }

    @Test
    void updateThrowsWhenScopeMissing() {
        DeviceContractTerm existing = DeviceContractTerm.builder()
                .deviceContractTermId(21L)
                .title("Old")
                .content("Old content")
                .active(true)
                .build();
        when(termRepository.findById(21L)).thenReturn(Optional.of(existing));

        DeviceContractTermRequestDto invalid = new DeviceContractTermRequestDto();
        invalid.setTitle("x");
        invalid.setContent("y");
        invalid.setActive(true);

        assertThatThrownBy(() -> service.update(21L, invalid, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateThrowsWhenBothScopesProvided() {
        DeviceContractTerm existing = DeviceContractTerm.builder()
                .deviceContractTermId(22L)
                .title("Old")
                .content("Old content")
                .active(true)
                .build();
        when(termRepository.findById(22L)).thenReturn(Optional.of(existing));

        DeviceContractTermRequestDto invalid = new DeviceContractTermRequestDto();
        invalid.setTitle("x");
        invalid.setContent("y");
        invalid.setActive(true);
        invalid.setDeviceModelId(1L);
        invalid.setDeviceCategoryId(2L);

        assertThatThrownBy(() -> service.update(22L, invalid, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateThrowsWhenModelNotFound() {
        DeviceContractTerm existing = DeviceContractTerm.builder()
                .deviceContractTermId(23L)
                .title("Old")
                .content("Old content")
                .active(true)
                .build();
        when(termRepository.findById(23L)).thenReturn(Optional.of(existing));
        when(deviceModelRepository.findById(999L)).thenReturn(Optional.empty());

        DeviceContractTermRequestDto req = new DeviceContractTermRequestDto();
        req.setTitle("x");
        req.setContent("y");
        req.setActive(true);
        req.setDeviceModelId(999L);

        assertThatThrownBy(() -> service.update(23L, req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy model thiết bị");
    }

    @Test
    void updateThrowsWhenCategoryNotFound() {
        DeviceContractTerm existing = DeviceContractTerm.builder()
                .deviceContractTermId(24L)
                .title("Old")
                .content("Old content")
                .active(true)
                .build();
        when(termRepository.findById(24L)).thenReturn(Optional.of(existing));
        when(deviceCategoryRepository.findById(888L)).thenReturn(Optional.empty());

        DeviceContractTermRequestDto req = new DeviceContractTermRequestDto();
        req.setTitle("x");
        req.setContent("y");
        req.setActive(true);
        req.setDeviceCategoryId(888L);

        assertThatThrownBy(() -> service.update(24L, req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy loại thiết bị");
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
    void findApplicableTermsReturnsEmptyWhenOrderOrDetailsNull() {
        assertThat(service.findApplicableTerms(null, List.of())).isEmpty();
        assertThat(service.findApplicableTerms(RentalOrder.builder().orderId(1L).build(), null)).isEmpty();
    }

    @Test
    void findApplicableTermsInfersCategoryFromModelsWhenMissingOnDetails() {
        // Given orderDetails contain models without category populated
        DeviceModel modelNoCat = DeviceModel.builder().deviceModelId(5L).deviceName("M5").build();
        RentalOrder order = RentalOrder.builder().orderId(2L).build();
        OrderDetail detail = OrderDetail.builder().deviceModel(modelNoCat).build();

        // When categoryIds are empty, service loads models to infer categories
        when(deviceModelRepository.findAllById(anyCollection()))
                .thenReturn(List.of(DeviceModel.builder().deviceModelId(5L).deviceCategory(category).build()));
        when(termRepository.findByDeviceCategory_DeviceCategoryIdInAndActiveIsTrue(anyCollection()))
                .thenReturn(List.of(DeviceContractTerm.builder().deviceContractTermId(20L).deviceCategory(category).build()));
        when(termRepository.findByDeviceModelIsNullAndDeviceCategoryIsNullAndActiveIsTrue())
                .thenReturn(List.of(DeviceContractTerm.builder().deviceContractTermId(21L).build()));

        List<DeviceContractTerm> result = service.findApplicableTerms(order, List.of(detail));

        assertThat(result).extracting(DeviceContractTerm::getDeviceContractTermId)
                .containsExactlyInAnyOrder(20L, 21L);
    }

    @Test
    void findApplicableTermsDeduplicatesOverlappingTerms() {
        RentalOrder order = RentalOrder.builder().orderId(3L).build();
        OrderDetail detail = OrderDetail.builder().deviceModel(model).build();

        DeviceContractTerm duplicated = DeviceContractTerm.builder().deviceContractTermId(30L).build();
        when(termRepository.findByDeviceModel_DeviceModelIdInAndActiveIsTrue(anyCollection()))
                .thenReturn(List.of(duplicated));
        when(termRepository.findByDeviceCategory_DeviceCategoryIdInAndActiveIsTrue(anyCollection()))
                .thenReturn(List.of(duplicated));
        when(termRepository.findByDeviceModelIsNullAndDeviceCategoryIsNullAndActiveIsTrue())
                .thenReturn(List.of(DeviceContractTerm.builder().deviceContractTermId(31L).build()));

        List<DeviceContractTerm> result = service.findApplicableTerms(order, List.of(detail));

        // Expect de-duplication by id => only two terms remain (30, 31)
        assertThat(result).extracting(DeviceContractTerm::getDeviceContractTermId)
                .containsExactlyInAnyOrder(30L, 31L);
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
