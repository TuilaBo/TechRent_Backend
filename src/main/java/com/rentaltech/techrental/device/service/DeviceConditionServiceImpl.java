package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.ConditionDefinition;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceCondition;
import com.rentaltech.techrental.device.model.dto.DeviceConditionRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceConditionResponseDto;
import com.rentaltech.techrental.device.repository.ConditionDefinitionRepository;
import com.rentaltech.techrental.device.repository.DeviceConditionRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceConditionServiceImpl implements DeviceConditionService {

    private final DeviceConditionRepository deviceConditionRepository;
    private final DeviceRepository deviceRepository;
    private final ConditionDefinitionRepository conditionDefinitionRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceConditionResponseDto> getByDevice(Long deviceId) {
        if (deviceId == null) {
            return List.of();
        }
        return deviceConditionRepository.findByDevice_DeviceId(deviceId).stream()
                .map(DeviceConditionResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<DeviceConditionResponseDto>> getByDeviceIds(Collection<Long> deviceIds) {
        if (CollectionUtils.isEmpty(deviceIds)) {
            return Collections.emptyMap();
        }
        List<DeviceCondition> all = deviceConditionRepository.findByDevice_DeviceIdIn(deviceIds);
        return all.stream()
                .filter(dc -> dc.getDevice() != null && dc.getDevice().getDeviceId() != null)
                .map(DeviceConditionResponseDto::from)
                .collect(Collectors.groupingBy(DeviceConditionResponseDto::getDeviceId));
    }

    @Override
    public List<DeviceConditionResponseDto> upsertConditions(Long deviceId,
                                                            List<DeviceConditionRequestDto> requests,
                                                            Long capturedByStaffId) {
        if (CollectionUtils.isEmpty(requests) || deviceId == null) {
            return getByDevice(deviceId);
        }
        Device device = getDevice(deviceId);
        Staff staff = resolveStaff(capturedByStaffId);
        Set<Long> definitionIds = requests.stream()
                .filter(Objects::nonNull)
                .map(DeviceConditionRequestDto::getConditionDefinitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ConditionDefinition> definitions = conditionDefinitionRepository.findAllById(definitionIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ConditionDefinition::getConditionDefinitionId, def -> def));

        List<DeviceConditionResponseDto> responses = new ArrayList<>();
        for (DeviceConditionRequestDto request : requests) {
            if (request == null || request.getConditionDefinitionId() == null) {
                continue;
            }
            ConditionDefinition definition = definitions.get(request.getConditionDefinitionId());
            if (definition == null) {
                throw new NoSuchElementException("Không tìm thấy condition definition: " + request.getConditionDefinitionId());
            }
            DeviceCondition saved = createCondition(device, definition, staff, request.getSeverity(), request.getNote(), request.getImages());
            responses.add(DeviceConditionResponseDto.from(saved));
        }
        return responses;
    }

    @Override
    public void updateCondition(Long deviceId,
                                Long conditionDefinitionId,
                                String severity,
                                String note,
                                List<String> images,
                                Long capturedByStaffId) {
        if (deviceId == null || conditionDefinitionId == null) {
            return;
        }
        Device device = getDevice(deviceId);
        ConditionDefinition definition = conditionDefinitionRepository.findById(conditionDefinitionId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy condition definition: " + conditionDefinitionId));
        Staff staff = resolveStaff(capturedByStaffId);
        createCondition(device, definition, staff, severity, note, images);
    }

    @Override
    public void deleteByDevice(Long deviceId) {
        if (deviceId == null) {
            return;
        }
        deviceConditionRepository.deleteByDevice_DeviceId(deviceId);
    }

    private DeviceCondition createCondition(Device device,
                                            ConditionDefinition definition,
                                            Staff staff,
                                            String severity,
                                            String note,
                                            List<String> images) {
        DeviceCondition entity = DeviceCondition.builder()
                .device(device)
                .conditionDefinition(definition)
                .severity(severity)
                .note(note)
                .images(images == null ? new ArrayList<>() : new ArrayList<>(images))
                .capturedAt(LocalDateTime.now())
                .capturedBy(staff)
                .build();
        return deviceConditionRepository.save(entity);
    }

    private Device getDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thiết bị: " + deviceId));
    }

    private Staff resolveStaff(Long staffId) {
        if (staffId == null) {
            return null;
        }
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy staff: " + staffId));
    }
}
