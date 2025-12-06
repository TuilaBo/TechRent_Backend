package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.model.dto.DeviceConditionResponseDto;
import com.rentaltech.techrental.device.model.dto.DeviceRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.rentalorder.repository.BookingCalendarRepository;
import com.rentaltech.techrental.rentalorder.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private DeviceRepository repository;
    @Mock
    private DeviceModelRepository deviceModelRepository;
    @Mock
    private AllocationRepository allocationRepository;
    @Mock
    private BookingCalendarRepository bookingCalendarRepository;
    @Mock
    private ReservationService reservationService;
    @Mock
    private DeviceConditionService deviceConditionService;

    @InjectMocks
    private DeviceServiceImpl service;

    private DeviceRequestDto baseRequest;
    private DeviceModel model;

    @BeforeEach
    void setUp() {
        baseRequest = DeviceRequestDto.builder()
                .serialNumber("SN-1")
                .status(DeviceStatus.AVAILABLE)
                .deviceModelId(1L)
                .build();
        model = DeviceModel.builder().deviceModelId(1L).amountAvailable(0L).build();
    }

    @Test
    void createRejectsDuplicatedSerial() {
        when(repository.findBySerialNumber("SN-1")).thenReturn(Optional.of(Device.builder().build()));

        assertThatThrownBy(() -> service.create(baseRequest))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createIncrementsModelAvailability() {
        when(repository.findBySerialNumber("SN-1")).thenReturn(Optional.empty());
        when(deviceModelRepository.findById(1L)).thenReturn(Optional.of(model));
        when(repository.save(any(Device.class))).thenAnswer(inv -> {
            Device device = inv.getArgument(0);
            device.setDeviceId(5L);
            device.setDeviceModel(model);
            return device;
        });
        when(deviceModelRepository.save(model)).thenReturn(model);

        DeviceResponseDto response = service.create(baseRequest);

        assertThat(response.getDeviceId()).isEqualTo(5L);
        assertThat(model.getAmountAvailable()).isEqualTo(1L);
        verify(deviceModelRepository).save(model);
    }

    @Test
    void findAvailableByModelFiltersBusyDevices() {
        Device available = Device.builder().deviceId(1L).deviceModel(model).status(DeviceStatus.AVAILABLE).build();
        Device damaged = Device.builder().deviceId(2L).deviceModel(model).status(DeviceStatus.DAMAGED).build();
        when(repository.findByDeviceModel_DeviceModelId(1L)).thenReturn(List.of(available, damaged));
        when(bookingCalendarRepository.findBusyDeviceIdsByModelAndRange(eq(1L), any(), any(), any()))
                .thenReturn(Set.of(2L));
        when(reservationService.countActiveReservedQuantity(eq(1L), any(), any())).thenReturn(0L);
        when(deviceConditionService.getByDeviceIds(any()))
                .thenReturn(Map.of(1L, List.of(DeviceConditionResponseDto.builder().deviceId(1L).build())));

        List<DeviceResponseDto> result = service.findAvailableByModelWithinRange(
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1));

        assertThat(result).hasSize(1);
    }

    @Test
    void updateAdjustsAvailabilityBetweenModels() {
        DeviceModel otherModel = DeviceModel.builder().deviceModelId(2L).amountAvailable(0L).build();
        Device entity = Device.builder()
                .deviceId(10L)
                .serialNumber("SN-1")
                .status(DeviceStatus.AVAILABLE)
                .deviceModel(model)
                .build();
        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.findBySerialNumber("SN-NEW")).thenReturn(Optional.of(entity));
        when(deviceModelRepository.findById(1L)).thenReturn(Optional.of(model));
        when(deviceModelRepository.findById(2L)).thenReturn(Optional.of(otherModel));
        when(repository.save(entity)).thenAnswer(inv -> entity);
        DeviceRequestDto request = DeviceRequestDto.builder()
                .serialNumber("SN-NEW")
                .status(DeviceStatus.AVAILABLE)
                .deviceModelId(2L)
                .build();

        service.update(10L, request);

        assertThat(model.getAmountAvailable()).isZero();
        assertThat(otherModel.getAmountAvailable()).isEqualTo(1L);
        verify(deviceModelRepository).save(model);
        verify(deviceModelRepository).save(otherModel);
    }
}
