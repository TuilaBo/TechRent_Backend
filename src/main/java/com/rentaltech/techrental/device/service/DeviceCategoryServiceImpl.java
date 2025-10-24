package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.dto.DeviceCategoryRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceCategoryResponseDto;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class DeviceCategoryServiceImpl implements DeviceCategoryService {

    private final DeviceCategoryRepository repository;

    public DeviceCategoryServiceImpl(DeviceCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public DeviceCategoryResponseDto create(DeviceCategoryRequestDto request) {
        DeviceCategory entity = mapToEntity(request);
        return mapToDto(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceCategoryResponseDto findById(Long id) {
        DeviceCategory entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("DeviceCategory not found: " + id));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceCategoryResponseDto> findAll() {
        return repository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    public DeviceCategoryResponseDto update(Long id, DeviceCategoryRequestDto request) {
        DeviceCategory entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("DeviceCategory not found: " + id));
        applyUpdates(entity, request);
        return mapToDto(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NoSuchElementException("DeviceCategory not found: " + id);
        repository.deleteById(id);
    }

    private DeviceCategory mapToEntity(DeviceCategoryRequestDto request) {
        if (request == null) throw new IllegalArgumentException("DeviceCategoryRequestDto is null");
        return DeviceCategory.builder()
                .deviceCategoryName(request.getDeviceCategoryName())
                .description(request.getDescription())
                .isActive(request.isActive())
                .build();
    }

    private void applyUpdates(DeviceCategory entity, DeviceCategoryRequestDto request) {
        if (request == null) throw new IllegalArgumentException("DeviceCategoryRequestDto is null");
        entity.setDeviceCategoryName(request.getDeviceCategoryName());
        entity.setDescription(request.getDescription());
        entity.setActive(request.isActive());
        repository.save(entity);
    }

    private DeviceCategoryResponseDto mapToDto(DeviceCategory entity) {
        return DeviceCategoryResponseDto.builder()
                .deviceCategoryId(entity.getDeviceCategoryId())
                .deviceCategoryName(entity.getDeviceCategoryName())
                .description(entity.getDescription())
                .isActive(entity.isActive())
                .build();
    }
}
