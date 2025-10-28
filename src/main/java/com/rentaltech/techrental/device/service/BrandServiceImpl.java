package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Brand;
import com.rentaltech.techrental.device.model.dto.BrandRequestDto;
import com.rentaltech.techrental.device.model.dto.BrandResponseDto;
import com.rentaltech.techrental.device.repository.BrandRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class BrandServiceImpl implements BrandService {

    private final BrandRepository repository;

    public BrandServiceImpl(BrandRepository repository) {
        this.repository = repository;
    }

    @Override
    public BrandResponseDto create(BrandRequestDto request) {
        Brand entity = mapToEntity(request);
        return mapToDto(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponseDto findById(Long id) {
        Brand entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Brand not found: " + id));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponseDto> findAll() {
        return repository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrandResponseDto> search(String brandName, Boolean isActive, Pageable pageable) {
        Specification<Brand> spec = buildSpecification(brandName, isActive);
        return repository.findAll(spec, pageable).map(this::mapToDto);
    }

    @Override
    public BrandResponseDto update(Long id, BrandRequestDto request) {
        Brand entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Brand not found: " + id));
        applyUpdates(entity, request);
        return mapToDto(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NoSuchElementException("Brand not found: " + id);
        repository.deleteById(id);
    }

    private Brand mapToEntity(BrandRequestDto request) {
        if (request == null) throw new IllegalArgumentException("BrandRequestDto is null");
        return Brand.builder()
                .brandName(request.getBrandName())
                .description(request.getDescription())
                .isActive(request.isActive())
                .build();
    }

    private void applyUpdates(Brand entity, BrandRequestDto request) {
        if (request == null) throw new IllegalArgumentException("BrandRequestDto is null");
        entity.setBrandName(request.getBrandName());
        entity.setDescription(request.getDescription());
        entity.setActive(request.isActive());
        repository.save(entity);
    }

    private BrandResponseDto mapToDto(Brand entity) {
        return BrandResponseDto.builder()
                .brandId(entity.getBrandId())
                .brandName(entity.getBrandName())
                .description(entity.getDescription())
                .isActive(entity.isActive())
                .build();
    }

    private Specification<Brand> buildSpecification(String brandName, Boolean isActive) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (brandName != null && !brandName.isBlank()) {
                predicate.getExpressions().add(cb.like(cb.lower(root.get("brandName")), "%" + brandName.toLowerCase() + "%"));
            }
            if (isActive != null) {
                predicate.getExpressions().add(cb.equal(root.get("isActive"), isActive));
            }
            return predicate;
        };
    }
}

