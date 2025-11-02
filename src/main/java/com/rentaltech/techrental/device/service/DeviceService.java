package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.DeviceRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface DeviceService {
    DeviceResponseDto create(DeviceRequestDto request);
    DeviceResponseDto findById(Long id);
    List<DeviceResponseDto> findAll();
    List<DeviceResponseDto> findByModelId(Long deviceModelId);
    List<DeviceResponseDto> findByOrderDetail(Long orderDetailId);
    DeviceResponseDto findBySerialNumber(String serialNumber);
    Page<DeviceResponseDto> search(String serialNumber,
                                   String shelfCode,
                                   String status,
                                   Long deviceModelId,
                                   String brand,
                                   String deviceName,
                                   Pageable pageable);
    DeviceResponseDto update(Long id, DeviceRequestDto request);
    void delete(Long id);
}
