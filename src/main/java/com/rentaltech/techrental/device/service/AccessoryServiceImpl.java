// java
package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Accessory;
import com.rentaltech.techrental.device.model.AccessoryCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.dto.AccessoryRequestDto;
import com.rentaltech.techrental.device.model.dto.AccessoryResponseDto;
import com.rentaltech.techrental.device.repository.AccessoryCategoryRepository;
import com.rentaltech.techrental.device.repository.AccessoryRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class AccessoryServiceImpl implements AccessoryService {

    private final AccessoryRepository repository;
    private final AccessoryCategoryRepository accessoryCategoryRepository;
    private final DeviceModelRepository deviceModelRepository;

    public AccessoryServiceImpl(AccessoryRepository repository,
                                AccessoryCategoryRepository accessoryCategoryRepository,
                                DeviceModelRepository deviceModelRepository) {
        this.repository = repository;
        this.accessoryCategoryRepository = accessoryCategoryRepository;
        this.deviceModelRepository = deviceModelRepository;
    }

    @Override
    public AccessoryResponseDto create(AccessoryRequestDto request) {
        Accessory entity = mapToEntity(request);
        Accessory saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccessoryResponseDto findById(Long id) {
        Accessory entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Accessory not found: " + id));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessoryResponseDto> findAll() {
        return repository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    public AccessoryResponseDto update(Long id, AccessoryRequestDto request) {
        Accessory entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Accessory not found: " + id));
        applyUpdates(entity, request);
        return mapToDto(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NoSuchElementException("Accessory not found: " + id);
        repository.deleteById(id);
    }

    private Accessory mapToEntity(AccessoryRequestDto request) {
        if (request == null) throw new IllegalArgumentException("AccessoryRequestDto is null");
        if (request.getAccessoryCategoryId() == null) {
            throw new IllegalArgumentException("accessoryCategoryId is required");
        }

        AccessoryCategory category = accessoryCategoryRepository.findById(request.getAccessoryCategoryId())
                .orElseThrow(() -> new NoSuchElementException("AccessoryCategory not found: " + request.getAccessoryCategoryId()));

        DeviceModel model = null;
        if (request.getDeviceModelId() != null) {
            model = deviceModelRepository.findById(request.getDeviceModelId())
                    .orElseThrow(() -> new NoSuchElementException("DeviceModel not found: " + request.getDeviceModelId()));
        }

        return Accessory.builder()
                .accessoryName(request.getAccessoryName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .isActive(request.isActive())
                .accessoryCategory(category)
                .deviceModel(model)
                .build();
    }

    private void applyUpdates(Accessory entity, AccessoryRequestDto request) {
        throw new UnsupportedOperationException("TODO: implement Accessory mapping (update)");
    }

    private AccessoryResponseDto mapToDto(Accessory entity) {
        return AccessoryResponseDto.builder()
                .accessoryId(entity.getAccessoryId())
                .accessoryName(entity.getAccessoryName())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .isActive(entity.isActive())
                .accessoryCategoryId(entity.getAccessoryCategory() != null ? entity.getAccessoryCategory().getAccessoryCategoryId() : null)
                .deviceModelId(entity.getDeviceModel() != null ? entity.getDeviceModel().getDeviceModelId() : null)
                .build();
    }
}
