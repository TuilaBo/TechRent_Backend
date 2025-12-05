package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.AccessoryCategory;
import com.rentaltech.techrental.device.model.dto.AccessoryCategoryRequestDto;
import com.rentaltech.techrental.device.model.dto.AccessoryCategoryResponseDto;
import com.rentaltech.techrental.device.repository.AccessoryCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AccessoryCategoryServiceImplTest {

    @Mock
    private AccessoryCategoryRepository repository;

    @InjectMocks
    private AccessoryCategoryServiceImpl service;

    @Test
    void createRejectsNullRequest() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AccessoryCategoryRequestDto");
    }

    @Test
    void updateAppliesNewValues() {
        AccessoryCategory existing = AccessoryCategory.builder()
                .accessoryCategoryId(5L)
                .accessoryCategoryName("Old")
                .description("Desc")
                .isActive(false)
                .build();
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any(AccessoryCategory.class))).thenAnswer(inv -> inv.getArgument(0));
        AccessoryCategoryRequestDto request = AccessoryCategoryRequestDto.builder()
                .accessoryCategoryName("New")
                .description("Updated")
                .isActive(true)
                .build();

        AccessoryCategoryResponseDto response = service.update(5L, request);

        assertThat(response.getAccessoryCategoryName()).isEqualTo("New");
        assertThat(response.getDescription()).isEqualTo("Updated");
        assertThat(response.isActive()).isTrue();
        verify(repository).save(existing);
    }

    @Test
    void deleteThrowsWhenCategoryMissing() {
        when(repository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
