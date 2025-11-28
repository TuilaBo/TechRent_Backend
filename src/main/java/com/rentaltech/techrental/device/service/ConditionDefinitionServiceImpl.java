package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.ConditionDefinition;
import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.dto.ConditionDefinitionRequestDto;
import com.rentaltech.techrental.device.model.dto.ConditionDefinitionResponseDto;
import com.rentaltech.techrental.device.repository.ConditionDefinitionRepository;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class ConditionDefinitionServiceImpl implements ConditionDefinitionService {

    private final ConditionDefinitionRepository conditionDefinitionRepository;
    private final DeviceCategoryRepository deviceCategoryRepository;

    @Override
    public ConditionDefinitionResponseDto create(ConditionDefinitionRequestDto request) {
        ConditionDefinition entity = ConditionDefinition.builder()
                .name(request.getName())
                .deviceCategory(resolveDeviceCategory(request.getDeviceCategoryId()))
                .description(request.getDescription())
                .impactRate(request.getImpactRate())
                .damage(request.isDamage())
                .defaultCompensation(request.getDefaultCompensation())
                .build();
        return ConditionDefinitionResponseDto.from(conditionDefinitionRepository.save(entity));
    }

    @Override
    public ConditionDefinitionResponseDto update(Long id, ConditionDefinitionRequestDto request) {
        ConditionDefinition entity = conditionDefinitionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy condition definition: " + id));
        entity.setName(request.getName());
        entity.setDeviceCategory(resolveDeviceCategory(request.getDeviceCategoryId()));
        entity.setDescription(request.getDescription());
        entity.setImpactRate(request.getImpactRate());
        entity.setDamage(request.isDamage());
        entity.setDefaultCompensation(request.getDefaultCompensation());
        return ConditionDefinitionResponseDto.from(conditionDefinitionRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ConditionDefinitionResponseDto getById(Long id) {
        ConditionDefinition entity = conditionDefinitionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy condition definition: " + id));
        return ConditionDefinitionResponseDto.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConditionDefinitionResponseDto> getAll() {
        return conditionDefinitionRepository.findAll().stream()
                .map(ConditionDefinitionResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConditionDefinitionResponseDto> getByDeviceCategory(Long deviceCategoryId) {
        return conditionDefinitionRepository.findByDeviceCategory_DeviceCategoryId(deviceCategoryId).stream()
                .map(ConditionDefinitionResponseDto::from)
                .toList();
    }

    @Override
    public void delete(Long id) {
        if (!conditionDefinitionRepository.existsById(id)) {
            throw new NoSuchElementException("Không tìm thấy condition definition: " + id);
        }
        conditionDefinitionRepository.deleteById(id);
    }

    private DeviceCategory resolveDeviceCategory(Long deviceCategoryId) {
        if (deviceCategoryId == null) {
            return null;
        }
        return deviceCategoryRepository.findById(deviceCategoryId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy device category: " + deviceCategoryId));
    }

}
