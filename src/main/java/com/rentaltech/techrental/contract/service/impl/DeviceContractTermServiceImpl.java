package com.rentaltech.techrental.contract.service.impl;

import com.rentaltech.techrental.contract.model.DeviceContractTerm;
import com.rentaltech.techrental.contract.model.dto.DeviceContractTermRequestDto;
import com.rentaltech.techrental.contract.model.dto.DeviceContractTermResponseDto;
import com.rentaltech.techrental.contract.repository.DeviceContractTermRepository;
import com.rentaltech.techrental.contract.service.DeviceContractTermService;
import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceContractTermServiceImpl implements DeviceContractTermService {

    private final DeviceContractTermRepository termRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final DeviceCategoryRepository deviceCategoryRepository;

    @Override
    public DeviceContractTermResponseDto create(DeviceContractTermRequestDto request, Long adminId) {
        validateScope(request);
        DeviceModel deviceModel = resolveDeviceModel(request.getDeviceModelId());
        DeviceCategory category = resolveCategory(request.getDeviceCategoryId());
        DeviceContractTerm term = DeviceContractTerm.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .deviceModel(deviceModel)
                .deviceCategory(category)
                .active(Boolean.TRUE.equals(request.getActive()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(adminId)
                .updatedBy(adminId)
                .build();
        return DeviceContractTermResponseDto.from(termRepository.save(term));
    }

    @Override
    public DeviceContractTermResponseDto update(Long termId, DeviceContractTermRequestDto request, Long adminId) {
        DeviceContractTerm term = termRepository.findById(termId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy điều khoản với id " + termId));
        validateScope(request);
        term.setTitle(request.getTitle());
        term.setContent(request.getContent());
        term.setDeviceModel(resolveDeviceModel(request.getDeviceModelId()));
        term.setDeviceCategory(resolveCategory(request.getDeviceCategoryId()));
        term.setActive(Boolean.TRUE.equals(request.getActive()));
        term.setUpdatedAt(LocalDateTime.now());
        term.setUpdatedBy(adminId);
        return DeviceContractTermResponseDto.from(termRepository.save(term));
    }

    @Override
    public void delete(Long termId) {
        DeviceContractTerm term = termRepository.findById(termId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy điều khoản với id " + termId));
        termRepository.delete(term);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceContractTermResponseDto get(Long termId) {
        return termRepository.findById(termId)
                .map(DeviceContractTermResponseDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy điều khoản với id " + termId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceContractTermResponseDto> list(Long deviceModelId, Long deviceCategoryId, Boolean active) {
        List<DeviceContractTerm> terms;
        if (deviceModelId != null) {
            terms = termRepository.findByDeviceModel_DeviceModelId(deviceModelId);
        } else if (deviceCategoryId != null) {
            terms = termRepository.findByDeviceCategory_DeviceCategoryId(deviceCategoryId);
        } else {
            terms = termRepository.findAll();
        }
        if (active != null) {
            terms = terms.stream()
                    .filter(term -> Objects.equals(Boolean.TRUE.equals(term.getActive()), active))
                    .collect(Collectors.toList());
        }
        return terms.stream().map(DeviceContractTermResponseDto::from).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceContractTerm> findApplicableTerms(RentalOrder order, List<OrderDetail> orderDetails) {
        if (order == null || orderDetails == null) {
            return List.of();
        }

        Set<Long> deviceModelIds = orderDetails.stream()
                .map(OrderDetail::getDeviceModel)
                .filter(Objects::nonNull)
                .map(DeviceModel::getDeviceModelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> categoryIds = orderDetails.stream()
                .map(OrderDetail::getDeviceModel)
                .filter(Objects::nonNull)
                .map(DeviceModel::getDeviceCategory)
                .filter(Objects::nonNull)
                .map(DeviceCategory::getDeviceCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (categoryIds.isEmpty() && !deviceModelIds.isEmpty()) {
            deviceModelRepository.findAllById(deviceModelIds).stream()
                    .map(DeviceModel::getDeviceCategory)
                    .filter(Objects::nonNull)
                    .map(DeviceCategory::getDeviceCategoryId)
                    .filter(Objects::nonNull)
                    .forEach(categoryIds::add);
        }

        List<DeviceContractTerm> result = new ArrayList<>();
        if (!deviceModelIds.isEmpty()) {
            result.addAll(termRepository.findByDeviceModel_DeviceModelIdInAndActiveIsTrue(deviceModelIds));
        }
        if (!categoryIds.isEmpty()) {
            result.addAll(termRepository.findByDeviceCategory_DeviceCategoryIdInAndActiveIsTrue(categoryIds));
        }
        result.addAll(termRepository.findByDeviceModelIsNullAndDeviceCategoryIsNullAndActiveIsTrue());
        return deduplicate(result);
    }

    private DeviceModel resolveDeviceModel(Long deviceModelId) {
        if (deviceModelId == null) {
            return null;
        }
        return deviceModelRepository.findById(deviceModelId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy model thiết bị với id " + deviceModelId));
    }

    private DeviceCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return deviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại thiết bị với id " + categoryId));
    }

    private void validateScope(DeviceContractTermRequestDto request) {
        if (request.getDeviceModelId() == null && request.getDeviceCategoryId() == null) {
            throw new IllegalArgumentException("Cần chọn model thiết bị hoặc loại thiết bị cho điều khoản");
        }
        if (request.getDeviceModelId() != null && request.getDeviceCategoryId() != null) {
            throw new IllegalArgumentException("Chỉ được chọn một trong hai: model thiết bị hoặc loại thiết bị");
        }
    }

    private List<DeviceContractTerm> deduplicate(List<DeviceContractTerm> terms) {
        Map<Long, DeviceContractTerm> indexed = new LinkedHashMap<>();
        for (DeviceContractTerm term : terms) {
            indexed.put(term.getDeviceContractTermId(), term);
        }
        return new ArrayList<>(indexed.values());
    }
}

