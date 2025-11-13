package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.model.dto.DeviceRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.rentalorder.model.BookingStatus;
import com.rentaltech.techrental.rentalorder.repository.BookingCalendarRepository;
import com.rentaltech.techrental.rentalorder.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository repository;
    private final DeviceModelRepository deviceModelRepository;
    private final AllocationRepository allocationRepository;
    private final BookingCalendarRepository bookingCalendarRepository;
    private final ReservationService reservationService;

    public DeviceServiceImpl(DeviceRepository repository,
                             DeviceModelRepository deviceModelRepository,
                             AllocationRepository allocationRepository,
                             BookingCalendarRepository bookingCalendarRepository,
                             ReservationService reservationService) {
        this.repository = repository;
        this.deviceModelRepository = deviceModelRepository;
        this.allocationRepository = allocationRepository;
        this.bookingCalendarRepository = bookingCalendarRepository;
        this.reservationService = reservationService;
    }

    @Override
    public DeviceResponseDto create(DeviceRequestDto request) {
        if (repository.findBySerialNumber(request.getSerialNumber()).isPresent()) {
            throw new IllegalArgumentException("Số serial đã tồn tại: " + request.getSerialNumber());
        }
        Device entity = mapToEntity(request);
        Device saved = repository.save(entity);

        // If new device is AVAILABLE, increase amountAvailable for its model
        if (saved.getStatus() == DeviceStatus.AVAILABLE && saved.getDeviceModel() != null) {
            Long modelId = saved.getDeviceModel().getDeviceModelId();
            deviceModelRepository.findById(modelId).ifPresent(model -> {
                Long current = model.getAmountAvailable() == null ? 0L : model.getAmountAvailable();
                model.setAmountAvailable(current + 1);
                deviceModelRepository.save(model);
            });
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponseDto findById(Long id) {
        Device entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thiết bị: " + id));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> findAll() {
        return repository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> findByModelId(Long deviceModelId) {
        return repository.findByDeviceModel_DeviceModelId(deviceModelId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> findAvailableByModelWithinRange(Long deviceModelId, LocalDateTime start, LocalDateTime end) {
        if (deviceModelId == null || start == null || end == null || !start.isBefore(end)) {
            return List.of();
        }
        List<Device> devices = repository.findByDeviceModel_DeviceModelId(deviceModelId);
        if (devices.isEmpty()) {
            return List.of();
        }
        Set<Long> busyDeviceIds = bookingCalendarRepository.findBusyDeviceIdsByModelAndRange(
                deviceModelId,
                start,
                end,
                EnumSet.of(BookingStatus.BOOKED, BookingStatus.ACTIVE)
        );
        List<Device> freeDevices = devices.stream()
                .filter(device -> device.getDeviceId() != null && !busyDeviceIds.contains(device.getDeviceId()))
                .sorted(Comparator.comparing(Device::getDeviceId))
                .toList();
        long reservedCount = reservationService.countActiveReservedQuantity(deviceModelId, start, end);
        long limit = Math.max(freeDevices.size() - reservedCount, 0);
        if (limit <= 0) {
            return List.of();
        }
        return freeDevices.stream()
                .limit(limit)
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> findByOrderDetail(Long orderDetailId) {
        return allocationRepository.findByOrderDetail_OrderDetailId(orderDetailId).stream()
                .map(Allocation::getDevice)
                .filter(Objects::nonNull)
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponseDto findBySerialNumber(String serialNumber) {
        return repository.findBySerialNumber(serialNumber)
                .map(this::mapToDto)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thiết bị với serial: " + serialNumber));
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
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thiết bị: " + id));
        // Capture old status and model for adjustments
        DeviceStatus oldStatus = entity.getStatus();
        Long oldModelId = entity.getDeviceModel() != null ? entity.getDeviceModel().getDeviceModelId() : null;
        applyUpdates(entity, request);
        // After applyUpdates, entity contains new state
        DeviceStatus newStatus = entity.getStatus();
        Long newModelId = entity.getDeviceModel() != null ? entity.getDeviceModel().getDeviceModelId() : null;

        // Decrement from old model if it was AVAILABLE
        if (oldStatus == DeviceStatus.AVAILABLE && oldModelId != null) {
            deviceModelRepository.findById(oldModelId).ifPresent(model -> {
                Long current = model.getAmountAvailable() == null ? 0L : model.getAmountAvailable();
                model.setAmountAvailable(Math.max(0L, current - 1));
                deviceModelRepository.save(model);
            });
        }
        // Increment on new model if now AVAILABLE
        if (newStatus == DeviceStatus.AVAILABLE && newModelId != null) {
            deviceModelRepository.findById(newModelId).ifPresent(model -> {
                Long current = model.getAmountAvailable() == null ? 0L : model.getAmountAvailable();
                model.setAmountAvailable(current + 1);
                deviceModelRepository.save(model);
            });
        }

        return mapToDto(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NoSuchElementException("Không tìm thấy thiết bị: " + id);
        repository.deleteById(id);
    }

    private Device mapToEntity(DeviceRequestDto request) {
        if (request == null) throw new IllegalArgumentException("DeviceRequestDto không được để trống");
        if (request.getDeviceModelId() == null) {
            throw new IllegalArgumentException("Cần cung cấp deviceModelId");
        }

        DeviceModel model = deviceModelRepository.findById(request.getDeviceModelId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy DeviceModel: " + request.getDeviceModelId()));

        return Device.builder()
                .serialNumber(request.getSerialNumber())
                .acquireAt(request.getAcquireAt())
                .status(request.getStatus())
//                .shelfCode(request.getShelfCode())
                .deviceModel(model)
                .build();
    }

    private void applyUpdates(Device entity, DeviceRequestDto request) {
        if (request == null) throw new IllegalArgumentException("DeviceRequestDto không được để trống");
        if (request.getDeviceModelId() == null) {
            throw new IllegalArgumentException("Cần cung cấp deviceModelId");
        }
        if (repository.findBySerialNumber(request.getSerialNumber()).isPresent() &&
                !repository.findBySerialNumber(request.getSerialNumber()).get().getDeviceId().equals(entity.getDeviceId())) {
            throw new IllegalArgumentException("Số serial đã tồn tại: " + request.getSerialNumber());
        }

        DeviceModel model = deviceModelRepository.findById(request.getDeviceModelId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy DeviceModel: " + request.getDeviceModelId()));

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
