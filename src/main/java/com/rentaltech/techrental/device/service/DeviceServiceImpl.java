package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.dto.DeviceRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

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
                .shelfCode(request.getShelfCode())
                .deviceModel(model)
                .build();
    }

    private void applyUpdates(Device entity, DeviceRequestDto request) {
        throw new UnsupportedOperationException("TODO: implement Device mapping (update)");
    }

    private DeviceResponseDto mapToDto(Device entity) {
        return DeviceResponseDto.builder()
                .deviceId(entity.getDeviceId())
                .serialNumber(entity.getSerialNumber())
                .acquireAt(entity.getAcquireAt())
                .status(entity.getStatus())
                .shelfCode(entity.getShelfCode())
                .deviceModelId(entity.getDeviceModel() != null ? entity.getDeviceModel().getDeviceModelId() : null)
                .build();
    }
}
