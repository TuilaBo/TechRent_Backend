package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Brand;
import com.rentaltech.techrental.device.model.dto.BrandRequestDto;
import com.rentaltech.techrental.device.repository.BrandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock
    private BrandRepository repository;

    @InjectMocks
    private BrandServiceImpl service;

    @Test
    void createRequiresRequest() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateChangesBrandFields() {
        Brand existing = Brand.builder().brandId(3L).brandName("Old").build();
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenAnswer(inv -> existing);
        BrandRequestDto request = BrandRequestDto.builder()
                .brandName("New")
                .description("Desc")
                .isActive(true)
                .build();

        service.update(3L, request);

        verify(repository).save(existing);
        assertThat(existing.getBrandName()).isEqualTo("New");
    }

    @Test
    void searchUsesSpecification() {
        when(repository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Brand>>any(), any(Pageable.class))).thenReturn(Page.empty());
        Page<?> page = service.search("Brand", true, Pageable.unpaged());
        assertThat(page.getTotalElements()).isZero();
        verify(repository).findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Brand>>any(), any(Pageable.class));
    }
}
