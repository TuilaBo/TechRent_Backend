package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.model.dto.DeviceConditionResponseDto;
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
    private final DeviceConditionService deviceConditionService;

    public DeviceServiceImpl(DeviceRepository repository,
                             DeviceModelRepository deviceModelRepository,
                             AllocationRepository allocationRepository,
                             BookingCalendarRepository bookingCalendarRepository,
                             ReservationService reservationService,
                             DeviceConditionService deviceConditionService) {
        this.repository = repository;
        this.deviceModelRepository = deviceModelRepository;
        this.allocationRepository = allocationRepository;
        this.bookingCalendarRepository = bookingCalendarRepository;
        this.reservationService = reservationService;
        this.deviceConditionService = deviceConditionService;
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

        return DeviceResponseDto.from(saved, deviceConditionService.getByDevice(saved.getDeviceId()));
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponseDto findById(Long id) {
        Device entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thiết bị: " + id));
        return DeviceResponseDto.from(entity, deviceConditionService.getByDevice(entity.getDeviceId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> findAll() {
        return mapDevicesWithConditions(repository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> findByModelId(Long deviceModelId) {
        return mapDevicesWithConditions(repository.findByDeviceModel_DeviceModelId(deviceModelId));
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
                .filter(device -> device.getStatus() != DeviceStatus.DAMAGED && device.getStatus() != DeviceStatus.LOST)
                .sorted(Comparator.comparing(Device::getDeviceId))
                .toList();
        long reservedCount = reservationService.countActiveReservedQuantity(deviceModelId, start, end);
        long limit = Math.max(freeDevices.size() - reservedCount, 0);
        if (limit <= 0) {
            return List.of();
        }
        List<Device> limitedDevices = freeDevices.stream()
                .limit(limit)
                .toList();
        return mapDevicesWithConditions(limitedDevices);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> findByOrderDetail(Long orderDetailId) {
        List<Device> devices = allocationRepository.findByOrderDetail_OrderDetailId(orderDetailId).stream()
                .map(Allocation::getDevice)
                .filter(Objects::nonNull)
                .toList();
        return mapDevicesWithConditions(devices);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponseDto findBySerialNumber(String serialNumber) {
        Device device = repository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thiết bị với serial: " + serialNumber));
        return DeviceResponseDto.from(device, deviceConditionService.getByDevice(device.getDeviceId()));
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
        DeviceStatus statusFilter = parseStatus(status);
        Page<Device> page = repository.searchDevices(
                normalize(serialNumber),
                statusFilter,
                deviceModelId,
                normalize(brand),
                normalize(deviceName),
                pageable);
        Map<Long, List<DeviceConditionResponseDto>> conditionMap = deviceConditionService.getByDeviceIds(
                page.getContent().stream()
                        .map(Device::getDeviceId)
                        .filter(Objects::nonNull)
                        .toList());
        return page.map(device -> DeviceResponseDto.from(
                device,
                conditionMap.getOrDefault(device.getDeviceId(), List.of())));
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

        Device saved = repository.save(entity);
        return DeviceResponseDto.from(saved, deviceConditionService.getByDevice(saved.getDeviceId()));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new NoSuchElementException("Không tìm thấy thiết bị: " + id);
        repository.deleteById(id);
    }

    private List<DeviceResponseDto> mapDevicesWithConditions(List<Device> devices) {
        if (devices == null || devices.isEmpty()) {
            return List.of();
        }
        Map<Long, List<DeviceConditionResponseDto>> conditionMap = deviceConditionService.getByDeviceIds(
                devices.stream()
                        .map(Device::getDeviceId)
                        .filter(Objects::nonNull)
                        .toList());
        return devices.stream()
                .map(device -> DeviceResponseDto.from(
                        device,
                        conditionMap.getOrDefault(device.getDeviceId(), List.of())))
                .toList();
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

    private DeviceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return DeviceStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
