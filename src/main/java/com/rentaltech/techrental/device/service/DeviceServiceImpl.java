package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.model.dto.DeviceRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;

@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository repository;
    private final DeviceModelRepository deviceModelRepository;

    public DeviceServiceImpl(DeviceRepository repository,
                             DeviceModelRepository deviceModelRepository) {
        this.repository = repository;
        this.deviceModelRepository = deviceModelRepository;
    }

    @Override
    public DeviceResponseDto create(DeviceRequestDto request) {
        if (repository.findBySerialNumber(request.getSerialNumber()).isPresent()) {
            throw new IllegalArgumentException("Serial number already exists: " + request.getSerialNumber());
        }
        Device entity = mapToEntity(request);
        return mapToDto(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponseDto findById(Long id) {
        Device entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Device not found: " + id));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> findAll() {
        return repository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeviceResponseDto> search(String serialNumber,
                                          String shelfCode,
                                          String status,
                                          Long deviceModelId,
                                          String brand,
                                          String deviceName,
                                          Pageable pageable) {
        Specification<Device> spec = buildSpecification(serialNumber, shelfCode, status, deviceModelId, brand, deviceName);
        return repository.findAll(spec, pageable).map(this::mapToDto);
    }

    @Override
    public DeviceResponseDto update(Long id, DeviceRequestDto request) {
        Device entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Device not found: " + id));
        applyUpdates(entity, request);
        return mapToDto(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NoSuchElementException("Device not found: " + id);
        repository.deleteById(id);
    }

    private Device mapToEntity(DeviceRequestDto request) {
        if (request == null) throw new IllegalArgumentException("DeviceRequestDto is null");
        if (request.getDeviceModelId() == null) {
            throw new IllegalArgumentException("deviceModelId is required");
        }

        DeviceModel model = deviceModelRepository.findById(request.getDeviceModelId())
                .orElseThrow(() -> new NoSuchElementException("DeviceModel not found: " + request.getDeviceModelId()));

        return Device.builder()
                .serialNumber(request.getSerialNumber())
                .acquireAt(request.getAcquireAt())
                .status(request.getStatus())
//                .shelfCode(request.getShelfCode())
                .deviceModel(model)
                .build();
    }

    private void applyUpdates(Device entity, DeviceRequestDto request) {
        if (request == null) throw new IllegalArgumentException("DeviceRequestDto is null");
        if (request.getDeviceModelId() == null) {
            throw new IllegalArgumentException("deviceModelId is required");
        }
        if (repository.findBySerialNumber(request.getSerialNumber()).isPresent() &&
                !repository.findBySerialNumber(request.getSerialNumber()).get().getDeviceId().equals(entity.getDeviceId())) {
            throw new IllegalArgumentException("Serial number already exists: " + request.getSerialNumber());
        }

        DeviceModel model = deviceModelRepository.findById(request.getDeviceModelId())
                .orElseThrow(() -> new NoSuchElementException("DeviceModel not found: " + request.getDeviceModelId()));

//        entity.setShelfCode(request.getShelfCode());
        entity.setSerialNumber(request.getSerialNumber());
        entity.setStatus(request.getStatus());
        entity.setDeviceModel(model);
        repository.save(entity);
    }

    private DeviceResponseDto mapToDto(Device entity) {
        return DeviceResponseDto.builder()
                .deviceId(entity.getDeviceId())
                .serialNumber(entity.getSerialNumber())
                .acquireAt(entity.getAcquireAt())
                .status(entity.getStatus())
//                .shelfCode(entity.getShelfCode())
                .deviceModelId(entity.getDeviceModel() != null ? entity.getDeviceModel().getDeviceModelId() : null)
                .build();
    }

    private Specification<Device> buildSpecification(String serialNumber,
                                                     String shelfCode,
                                                     String status,
                                                     Long deviceModelId,
                                                     String brand,
                                                     String deviceName) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (serialNumber != null && !serialNumber.isBlank()) {
                predicate.getExpressions().add(cb.like(cb.lower(root.get("serialNumber")), "%" + serialNumber.toLowerCase() + "%"));
            }
            if (shelfCode != null && !shelfCode.isBlank()) {
                predicate.getExpressions().add(cb.like(cb.lower(root.get("shelfCode")), "%" + shelfCode.toLowerCase() + "%"));
            }
            if (status != null && !status.isBlank()) {
                try {
                    DeviceStatus st = DeviceStatus.valueOf(status.toUpperCase());
                    predicate.getExpressions().add(cb.equal(root.get("status"), st));
                } catch (IllegalArgumentException ignored) {}
            }
            if (deviceModelId != null) {
                predicate.getExpressions().add(cb.equal(root.join("deviceModel").get("deviceModelId"), deviceModelId));
            }
            if (brand != null && !brand.isBlank()) {
                predicate.getExpressions().add(cb.like(cb.lower(root.join("deviceModel").get("brand")), "%" + brand.toLowerCase() + "%"));
            }
            if (deviceName != null && !deviceName.isBlank()) {
                predicate.getExpressions().add(cb.like(cb.lower(root.join("deviceModel").get("deviceName")), "%" + deviceName.toLowerCase() + "%"));
            }
            return predicate;
        };
    }
}
