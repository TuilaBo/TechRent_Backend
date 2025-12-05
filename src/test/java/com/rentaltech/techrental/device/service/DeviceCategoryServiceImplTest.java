package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.dto.DeviceCategoryRequestDto;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCategoryServiceImplTest {

    @Mock
    private DeviceCategoryRepository repository;

    @InjectMocks
    private DeviceCategoryServiceImpl service;

    @Test
    void createValidatesInput() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatePersistsChanges() {
        DeviceCategory category = DeviceCategory.builder().deviceCategoryId(1L).deviceCategoryName("Laptop").build();
        when(repository.findById(1L)).thenReturn(Optional.of(category));
        when(repository.save(category)).thenAnswer(inv -> category);
        DeviceCategoryRequestDto request = DeviceCategoryRequestDto.builder()
                .deviceCategoryName("Camera")
                .description("desc")
                .isActive(true)
                .build();

        service.update(1L, request);

        verify(repository).save(category);
        assertThat(category.getDeviceCategoryName()).isEqualTo("Camera");
    }

    @Test
    void searchDelegatesToRepository() {
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<DeviceCategory>>any(), any(Pageable.class))).thenReturn(Page.empty());
        assertThat(service.search(null, null, Pageable.unpaged()).getTotalElements()).isZero();
    }
}
