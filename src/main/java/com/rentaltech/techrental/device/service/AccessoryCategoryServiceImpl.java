package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.AccessoryCategory;
import com.rentaltech.techrental.device.model.dto.AccessoryCategoryRequestDto;
import com.rentaltech.techrental.device.model.dto.AccessoryCategoryResponseDto;
import com.rentaltech.techrental.device.repository.AccessoryCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class AccessoryCategoryServiceImpl implements AccessoryCategoryService {

    private final AccessoryCategoryRepository repository;

    public AccessoryCategoryServiceImpl(AccessoryCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public AccessoryCategoryResponseDto create(AccessoryCategoryRequestDto request) {
        AccessoryCategory entity = mapToEntity(request);
        return AccessoryCategoryResponseDto.from(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public AccessoryCategoryResponseDto findById(Long id) {
        AccessoryCategory entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục phụ kiện: " + id));
        return AccessoryCategoryResponseDto.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessoryCategoryResponseDto> findAll() {
        return repository.findAll().stream().map(AccessoryCategoryResponseDto::from).toList();
    }

    @Override
    public AccessoryCategoryResponseDto update(Long id, AccessoryCategoryRequestDto request) {
        AccessoryCategory entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục phụ kiện: " + id));
        applyUpdates(entity, request);
        return AccessoryCategoryResponseDto.from(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NoSuchElementException("Không tìm thấy danh mục phụ kiện: " + id);
        repository.deleteById(id);
    }

    private AccessoryCategory mapToEntity(AccessoryCategoryRequestDto request) {
        if (request == null) throw new IllegalArgumentException("AccessoryCategoryRequestDto không được để trống");
        return AccessoryCategory.builder()
                .accessoryCategoryName(request.getAccessoryCategoryName())
                .description(request.getDescription())
                .isActive(request.isActive())
                .build();
    }

    private void applyUpdates(AccessoryCategory entity, AccessoryCategoryRequestDto request) {
        if (request == null) throw new IllegalArgumentException("AccessoryCategoryRequestDto không được để trống");

        entity.setAccessoryCategoryName(request.getAccessoryCategoryName());
        entity.setDescription(request.getDescription());
        entity.setActive(request.isActive());
        repository.save(entity);
    }

}
