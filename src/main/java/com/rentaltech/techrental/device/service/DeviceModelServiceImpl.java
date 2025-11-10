package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Brand;
import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.dto.DeviceModelRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceModelResponseDto;
import com.rentaltech.techrental.device.repository.BrandRepository;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final ImageStorageService imageStorageService;

    @Override
    public DeviceModelResponseDto create(DeviceModelRequestDto request, MultipartFile imageFile) {
        DeviceModel entity = mapToEntity(request);
        maybeUploadDeviceModelImage(imageFile, request, entity);
        return mapToDto(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceModelResponseDto findById(Long id) {
        DeviceModel entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy DeviceModel: " + id));
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
    public DeviceModelResponseDto update(Long id, DeviceModelRequestDto request, MultipartFile imageFile) {
        DeviceModel entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy DeviceModel: " + id));
        applyUpdates(entity, request);
        maybeUploadDeviceModelImage(imageFile, request, entity);
        return mapToDto(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NoSuchElementException("Không tìm thấy DeviceModel: " + id);
        repository.deleteById(id);
    }

    private DeviceModel mapToEntity(DeviceModelRequestDto request) {
        if (request == null) throw new IllegalArgumentException("DeviceModelRequestDto không được để trống");
        if (request.getDeviceCategoryId() == null) {
            throw new IllegalArgumentException("Cần cung cấp deviceCategoryId");
        }
        if (request.getBrandId() == null) {
            throw new IllegalArgumentException("Cần cung cấp brandId");
        }
        DeviceCategory category = deviceCategoryRepository.findById(request.getDeviceCategoryId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy DeviceCategory: " + request.getDeviceCategoryId()));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thương hiệu: " + request.getBrandId()));

        return DeviceModel.builder()
                .deviceName(request.getDeviceName())
                .brand(brand)
                .description(request.getDescription())
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
        if (request == null) throw new IllegalArgumentException("DeviceModelRequestDto không được để trống");
        if (request.getDeviceCategoryId() == null) {
            throw new IllegalArgumentException("Cần cung cấp deviceCategoryId");
        }
        if (request.getBrandId() == null) {
            throw new IllegalArgumentException("Cần cung cấp brandId");
        }
        DeviceCategory category = deviceCategoryRepository.findById(request.getDeviceCategoryId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy DeviceCategory: " + request.getDeviceCategoryId()));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thương hiệu: " + request.getBrandId()));

        entity.setDeviceName(request.getDeviceName());
        entity.setBrand(brand);
        entity.setDescription(request.getDescription());
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

    private void maybeUploadDeviceModelImage(MultipartFile imageFile, DeviceModelRequestDto request, DeviceModel entity) {
        if (imageFile == null || imageFile.isEmpty()) {
            return;
        }
        String uploadedUrl = imageStorageService.uploadDeviceModelImage(
                imageFile,
                request.getBrandId(),
                request.getDeviceName()
        );
        entity.setImageURL(uploadedUrl);
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
