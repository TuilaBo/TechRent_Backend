package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.dto.DeviceModelRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceModelResponseDto;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class DeviceModelServiceImpl implements DeviceModelService {

    private final DeviceModelRepository repository;
    private final DeviceCategoryRepository deviceCategoryRepository;

    public DeviceModelServiceImpl(DeviceModelRepository repository,
                                  DeviceCategoryRepository deviceCategoryRepository) {
        this.repository = repository;
        this.deviceCategoryRepository = deviceCategoryRepository;
    }

    @Override
    public DeviceModelResponseDto create(DeviceModelRequestDto request) {
        DeviceModel entity = mapToEntity(request);
        return mapToDto(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceModelResponseDto findById(Long id) {
        DeviceModel entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("DeviceModel not found: " + id));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceModelResponseDto> findAll() {
        return repository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    public DeviceModelResponseDto update(Long id, DeviceModelRequestDto request) {
        DeviceModel entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("DeviceModel not found: " + id));
        applyUpdates(entity, request);
        return mapToDto(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NoSuchElementException("DeviceModel not found: " + id);
        repository.deleteById(id);
    }

    private DeviceModel mapToEntity(DeviceModelRequestDto request) {
        if (request == null) throw new IllegalArgumentException("DeviceModelRequestDto is null");
        if (request.getDeviceCategoryId() == null) {
            throw new IllegalArgumentException("deviceCategoryId is required");
        }
        DeviceCategory category = deviceCategoryRepository.findById(request.getDeviceCategoryId())
                .orElseThrow(() -> new NoSuchElementException("DeviceCategory not found: " + request.getDeviceCategoryId()));

        return DeviceModel.builder()
                .deviceName(request.getDeviceName())
                .brand(request.getBrand())
                .imageURL(request.getImageURL())
                .specifications(request.getSpecifications())
                .isActive(request.isActive())
                .deviceCategory(category)
                .deviceValue(request.getDeviceValue())
                .pricePerDay(request.getPricePerDay())
                .depositPercent(request.getDepositPercent())
                .build();
    }

    private void applyUpdates(DeviceModel entity, DeviceModelRequestDto request) {
        if (request == null) throw new IllegalArgumentException("DeviceModelRequestDto is null");
        if (request.getDeviceCategoryId() == null) {
            throw new IllegalArgumentException("deviceCategoryId is required");
        }
        DeviceCategory category = deviceCategoryRepository.findById(request.getDeviceCategoryId())
                .orElseThrow(() -> new NoSuchElementException("DeviceCategory not found: " + request.getDeviceCategoryId()));

        entity.setDeviceName(request.getDeviceName());
        entity.setBrand(request.getBrand());
        entity.setImageURL(request.getImageURL());
        entity.setSpecifications(request.getSpecifications());
        entity.setActive(request.isActive());
        entity.setDeviceCategory(category);
        entity.setDeviceValue(request.getDeviceValue());
        entity.setPricePerDay(request.getPricePerDay());
        entity.setDepositPercent(request.getDepositPercent());
    }

    private DeviceModelResponseDto mapToDto(DeviceModel entity) {
        return DeviceModelResponseDto.builder()
                .deviceModelId(entity.getDeviceModelId())
                .deviceName(entity.getDeviceName())
                .brand(entity.getBrand())
                .imageURL(entity.getImageURL())
                .specifications(entity.getSpecifications())
                .isActive(entity.isActive())
                .deviceCategoryId(entity.getDeviceCategory() != null ? entity.getDeviceCategory().getDeviceCategoryId() : null)
                .deviceValue(entity.getDeviceValue())
                .pricePerDay(entity.getPricePerDay())
                .depositPercent(entity.getDepositPercent())
                .build();
    }
}
