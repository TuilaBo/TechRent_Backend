package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.Brand;
import com.rentaltech.techrental.device.model.dto.DeviceModelRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceModelResponseDto;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.BrandRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
@RequiredArgsConstructor
public class DeviceModelServiceImpl implements DeviceModelService {

    private final DeviceRepository deviceRepository;
    private final DeviceModelRepository repository;
    private final DeviceCategoryRepository deviceCategoryRepository;
    private final BrandRepository brandRepository;

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
    @Transactional(readOnly = true)
    public Page<DeviceModelResponseDto> search(String deviceName,
                                               Long brandId,
                                               Long amountAvailable,
                                               Long deviceCategoryId,
                                               BigDecimal pricePerDay,
                                               Boolean isActive,
                                               Pageable pageable) {
        Specification<DeviceModel> spec = buildSpecification(deviceName, brandId, amountAvailable, deviceCategoryId, pricePerDay, isActive);
        return repository.findAll(spec, pageable).map(this::mapToDto);
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
        if (request.getBrandId() == null) {
            throw new IllegalArgumentException("brandId is required");
        }
        DeviceCategory category = deviceCategoryRepository.findById(request.getDeviceCategoryId())
                .orElseThrow(() -> new NoSuchElementException("DeviceCategory not found: " + request.getDeviceCategoryId()));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new NoSuchElementException("Brand not found: " + request.getBrandId()));

        return DeviceModel.builder()
                .deviceName(request.getDeviceName())
                .brand(brand)
                .description(request.getDescription())
                .imageURL(request.getImageURL())
                .amountAvailable(0L)
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
        if (request.getBrandId() == null) {
            throw new IllegalArgumentException("brandId is required");
        }
        DeviceCategory category = deviceCategoryRepository.findById(request.getDeviceCategoryId())
                .orElseThrow(() -> new NoSuchElementException("DeviceCategory not found: " + request.getDeviceCategoryId()));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new NoSuchElementException("Brand not found: " + request.getBrandId()));

        entity.setDeviceName(request.getDeviceName());
        entity.setBrand(brand);
        entity.setDescription(request.getDescription());
        entity.setImageURL(request.getImageURL());
        entity.setSpecifications(request.getSpecifications());
        entity.setAmountAvailable(deviceRepository.countByDeviceModel_DeviceModelId(entity.getDeviceModelId()));
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
                .description(entity.getDescription())
                .amountAvailable(entity.getAmountAvailable())
                .brandId(entity.getBrand() != null ? entity.getBrand().getBrandId() : null)
                .imageURL(entity.getImageURL())
                .specifications(entity.getSpecifications())
                .isActive(entity.isActive())
                .deviceCategoryId(entity.getDeviceCategory() != null ? entity.getDeviceCategory().getDeviceCategoryId() : null)
                .deviceValue(entity.getDeviceValue())
                .pricePerDay(entity.getPricePerDay())
                .depositPercent(entity.getDepositPercent())
                .build();
    }

    private Specification<DeviceModel> buildSpecification(String deviceName,
                                                          Long brandId,
                                                          Long amountAvailable,
                                                          Long deviceCategoryId,
                                                          BigDecimal pricePerDay,
                                                          Boolean isActive) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (deviceName != null && !deviceName.isBlank()) {
                predicate.getExpressions().add(cb.like(cb.lower(root.get("deviceName")), "%" + deviceName.toLowerCase() + "%"));
            }
            if (brandId != null) {
                predicate.getExpressions().add(cb.equal(root.join("brand").get("brandId"), brandId));
            }
            if (amountAvailable != null) {
                predicate.getExpressions().add(cb.equal(root.join("amountAvailable"), amountAvailable));
            }
            if (pricePerDay != null) {
                predicate.getExpressions().add(cb.equal(root.join("pricePerDay"), pricePerDay));
            }
            if (deviceCategoryId != null) {
                predicate.getExpressions().add(cb.equal(root.join("deviceCategory").get("deviceCategoryId"), deviceCategoryId));
            }
            if (isActive != null) {
                predicate.getExpressions().add(cb.equal(root.get("isActive"), isActive));
            }
            return predicate;
        };
    }
}
