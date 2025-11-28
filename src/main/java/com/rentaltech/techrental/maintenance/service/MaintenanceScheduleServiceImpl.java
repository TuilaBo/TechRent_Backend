package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.maintenance.model.MaintenancePlan;
import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.model.dto.*;
import com.rentaltech.techrental.maintenance.repository.MaintenancePlanRepository;
import com.rentaltech.techrental.maintenance.repository.MaintenanceScheduleCustomRepository;
import com.rentaltech.techrental.maintenance.repository.MaintenanceScheduleRepository;
import com.rentaltech.techrental.rentalorder.model.BookingCalendar;
import com.rentaltech.techrental.rentalorder.model.BookingStatus;
import com.rentaltech.techrental.rentalorder.repository.BookingCalendarRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceScheduleServiceImpl implements MaintenanceScheduleService {

    private final MaintenanceScheduleRepository scheduleRepository;
    private final MaintenanceScheduleCustomRepository scheduleCustomRepository;
    private final MaintenancePlanRepository planRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCategoryRepository deviceCategoryRepository;
    private final BookingCalendarRepository bookingCalendarRepository;

    private static final long RENTAL_CONFLICT_LOOKAHEAD_DAYS = 14L;

    @Override
    @Transactional
    public MaintenanceSchedule createSchedule(Long deviceId, Long maintenancePlanId, LocalDate startDate, LocalDate endDate, String status) {
        Device device = deviceRepository.findById(deviceId).orElseThrow();

        MaintenancePlan plan = null;
        if (maintenancePlanId != null) {
            plan = planRepository.findById(maintenancePlanId).orElseThrow();
        }

        String scheduleStatus = status != null ? status : "SCHEDULED";
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .device(device)
                .maintenancePlan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(scheduleStatus)
                .createdAt(LocalDateTime.now())
                .build();
        MaintenanceSchedule saved = scheduleRepository.save(schedule);

        // Update device status if maintenance is starting now or in progress
        if (startDate != null && (startDate.isBefore(LocalDate.now()) || startDate.isEqual(LocalDate.now()))) {
            if ("IN_PROGRESS".equalsIgnoreCase(scheduleStatus) || "STARTED".equalsIgnoreCase(scheduleStatus)) {
                device.setStatus(DeviceStatus.UNDER_MAINTENANCE);
                deviceRepository.save(device);
            }
        }

        return saved;
    }

    @Override
    public List<MaintenanceSchedule> listByDevice(Long deviceId) {
        return scheduleRepository.findByDevice_DeviceId(deviceId);
    }

    @Override
    @Transactional
    public MaintenanceSchedule updateStatus(Long maintenanceScheduleId, String status) {
        MaintenanceSchedule schedule = scheduleRepository.findById(maintenanceScheduleId).orElseThrow();
        Device device = schedule.getDevice();
        schedule.setStatus(status);
        MaintenanceSchedule saved = scheduleRepository.save(schedule);

        // Update device status based on maintenance schedule status
        if (device != null) {
            if ("IN_PROGRESS".equalsIgnoreCase(status) || "STARTED".equalsIgnoreCase(status)) {
                // Start maintenance - set device to UNDER_MAINTENANCE
                device.setStatus(DeviceStatus.UNDER_MAINTENANCE);
                deviceRepository.save(device);
            } else if ("COMPLETED".equalsIgnoreCase(status) || "FINISHED".equalsIgnoreCase(status)) {
                // Maintenance completed - set device back to AVAILABLE
                device.setStatus(DeviceStatus.AVAILABLE);
                deviceRepository.save(device);
            } else if ("CANCELLED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {
                // Maintenance cancelled - restore device to previous status if it was UNDER_MAINTENANCE
                if (device.getStatus() == DeviceStatus.UNDER_MAINTENANCE) {
                    device.setStatus(DeviceStatus.AVAILABLE);
                    deviceRepository.save(device);
                }
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public List<MaintenanceSchedule> createSchedulesByCategory(MaintenanceScheduleByCategoryRequestDto request) {
        DeviceCategory category = deviceCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));

        List<Device> devices = deviceRepository.findAll().stream()
                .filter(device -> device.getDeviceModel() != null
                        && device.getDeviceModel().getDeviceCategory() != null
                        && device.getDeviceModel().getDeviceCategory().getDeviceCategoryId().equals(category.getDeviceCategoryId()))
                .collect(Collectors.toList());

        if (devices.isEmpty()) {
            throw new IllegalArgumentException("No devices found for category: " + category.getDeviceCategoryName());
        }

        LocalDate endDate = request.getEndDate();
        if (request.getDurationDays() != null && request.getDurationDays() > 0) {
            endDate = request.getStartDate().plusDays(request.getDurationDays() - 1);
        }

        List<MaintenanceSchedule> schedules = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String scheduleStatus = request.getStatus() != null ? request.getStatus() : "SCHEDULED";
        
        for (Device device : devices) {
            MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                    .device(device)
                    .maintenancePlan(null)
                    .startDate(request.getStartDate())
                    .endDate(endDate)
                    .status(scheduleStatus)
                    .createdAt(LocalDateTime.now())
                    .build();
            MaintenanceSchedule saved = scheduleRepository.save(schedule);
            schedules.add(saved);

            // Update device status if maintenance starts today or earlier
            if (request.getStartDate() != null && 
                (request.getStartDate().isBefore(today) || request.getStartDate().isEqual(today))) {
                if ("IN_PROGRESS".equalsIgnoreCase(scheduleStatus) || "STARTED".equalsIgnoreCase(scheduleStatus)) {
                    device.setStatus(DeviceStatus.UNDER_MAINTENANCE);
                    deviceRepository.save(device);
                }
            }
        }

        return schedules;
    }

    @Override
    @Transactional
    public List<MaintenanceSchedule> createSchedulesByUsage(MaintenanceScheduleByUsageRequestDto request) {
        List<Device> devices = deviceRepository.findAll().stream()
                .filter(device -> device.getUsageCount() != null
                        && device.getUsageCount() >= request.getUsageCount())
                .collect(Collectors.toList());

        if (devices.isEmpty()) {
            throw new IllegalArgumentException("No devices found with usage count >= " + request.getUsageCount());
        }

        LocalDate endDate = request.getEndDate();
        if (request.getDurationDays() != null && request.getDurationDays() > 0) {
            endDate = request.getStartDate().plusDays(request.getDurationDays() - 1);
        }

        List<MaintenanceSchedule> schedules = new ArrayList<>();
        for (Device device : devices) {
            MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                    .device(device)
                    .maintenancePlan(null)
                    .startDate(request.getStartDate())
                    .endDate(endDate)
                    .status(request.getStatus() != null ? request.getStatus() : "SCHEDULED")
                    .createdAt(LocalDateTime.now())
                    .build();
            schedules.add(scheduleRepository.save(schedule));
        }

        return schedules;
    }

    @Override
    @Transactional
    public List<MaintenanceConflictResponseDto> checkConflicts(MaintenanceConflictCheckRequestDto request) {
        List<MaintenanceConflictResponseDto> conflicts = new ArrayList<>();

        // Use criteria API to find all conflicting schedules at once
        List<MaintenanceSchedule> overlappingSchedules = scheduleCustomRepository.findConflictingSchedules(
                request.getDeviceIds(),
                request.getStartDate(),
                request.getEndDate()
        );

        // Group by device
        var schedulesByDevice = overlappingSchedules.stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getDevice().getDeviceId()));

        for (Long deviceId : request.getDeviceIds()) {
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

            List<MaintenanceSchedule> deviceSchedules = schedulesByDevice.getOrDefault(deviceId, List.of());

            if (!deviceSchedules.isEmpty()) {
                List<MaintenanceConflictResponseDto.ConflictInfo> conflictInfos = deviceSchedules.stream()
                        .map(schedule -> MaintenanceConflictResponseDto.ConflictInfo.builder()
                                .scheduleId(schedule.getMaintenanceScheduleId())
                                .scheduleStartDate(schedule.getStartDate())
                                .scheduleEndDate(schedule.getEndDate())
                                .scheduleStatus(schedule.getStatus())
                                .build())
                        .collect(Collectors.toList());

                DeviceModel model = device.getDeviceModel();
                String modelName = model != null ? model.getDeviceName() : null;
                
                MaintenanceConflictResponseDto conflict = MaintenanceConflictResponseDto.builder()
                        .deviceId(device.getDeviceId())
                        .deviceSerialNumber(device.getSerialNumber())
                        .deviceModelName(modelName)
                        .conflicts(conflictInfos)
                        .build();
                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    @Override
    @Transactional
    public List<PriorityMaintenanceDeviceDto> getPriorityMaintenanceDevices() {
        List<Device> allDevices = deviceRepository.findAll();
        List<PriorityMaintenanceDeviceDto> priorityDevices = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lookAhead = now.plusDays(RENTAL_CONFLICT_LOOKAHEAD_DAYS);
        List<BookingCalendar> upcomingBookings = bookingCalendarRepository.findUpcomingBookings(
                now,
                lookAhead,
                EnumSet.of(BookingStatus.BOOKED, BookingStatus.ACTIVE)
        );
        Map<Long, List<BookingCalendar>> bookingsByDevice = upcomingBookings.stream()
                .filter(booking -> booking.getDevice() != null && booking.getDevice().getDeviceId() != null)
                .collect(Collectors.groupingBy(booking -> booking.getDevice().getDeviceId()));

        List<MaintenancePlan> usagePlans = planRepository.findAll().stream()
                .filter(plan -> plan.getRuleType() != null
                        && plan.getRuleType().name().equals("ByUsage")
                        && plan.getActive() != null && plan.getActive())
                .collect(Collectors.toList());

        for (Device device : allDevices) {
            List<MaintenanceSchedule> upcomingSchedules = scheduleRepository.findByDevice_DeviceId(device.getDeviceId()).stream()
                    .filter(schedule -> {
                        LocalDate startDate = schedule.getStartDate();
                        return startDate != null && !startDate.isBefore(LocalDate.now());
                    })
                    .sorted(Comparator.comparing(MaintenanceSchedule::getStartDate, Comparator.nullsLast(LocalDate::compareTo)))
                    .collect(Collectors.toList());
            MaintenanceSchedule nextSchedule = upcomingSchedules.isEmpty() ? null : upcomingSchedules.get(0);
            Optional<BookingCalendar> nextBooking = Optional.ofNullable(bookingsByDevice.get(device.getDeviceId()))
                    .flatMap(bookings -> bookings.stream()
                            .sorted(Comparator.comparing(BookingCalendar::getStartTime))
                            .findFirst());

            if (nextSchedule != null && nextBooking.isPresent()
                    && hasRentalMaintenanceConflict(nextSchedule, nextBooking.get())) {
                priorityDevices.add(buildPriorityDeviceDto(device, nextSchedule, "RENTAL_CONFLICT",
                        nextBooking.get(), null));
                continue;
            }

            if (nextSchedule != null) {
                priorityDevices.add(buildPriorityDeviceDto(device, nextSchedule, "SCHEDULED_MAINTENANCE",
                        null, null));
                continue;
            }

            for (MaintenancePlan plan : usagePlans) {
                if (plan.getRuleValue() != null
                        && device.getUsageCount() != null
                        && device.getUsageCount() >= plan.getRuleValue()) {
                    priorityDevices.add(buildPriorityDeviceDto(device, null, "USAGE_THRESHOLD",
                            null, plan.getRuleValue()));
                    break;
                }
            }
        }

        return priorityDevices.stream()
                .sorted((d1, d2) -> {
                    if (d1.getNextMaintenanceDate() != null && d2.getNextMaintenanceDate() != null) {
                        return d1.getNextMaintenanceDate().compareTo(d2.getNextMaintenanceDate());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MaintenanceSchedule> getActiveMaintenanceSchedules() {
        return scheduleCustomRepository.findActiveMaintenanceSchedules();
    }

    private PriorityMaintenanceDeviceDto buildPriorityDeviceDto(Device device,
                                                                MaintenanceSchedule schedule,
                                                                String reason,
                                                                BookingCalendar conflictBooking,
                                                                Integer requiredUsageCount) {
        DeviceModel model = device.getDeviceModel();
        PriorityMaintenanceDeviceDto.PriorityMaintenanceDeviceDtoBuilder builder = PriorityMaintenanceDeviceDto.builder()
                .deviceId(device.getDeviceId())
                .deviceSerialNumber(device.getSerialNumber())
                .deviceModelName(model != null ? model.getDeviceName() : null)
                .deviceCategoryName(model != null && model.getDeviceCategory() != null
                        ? model.getDeviceCategory().getDeviceCategoryName() : null)
                .currentUsageCount(device.getUsageCount() != null ? device.getUsageCount() : 0)
                .priorityReason(reason);

        if (schedule != null) {
            builder.nextMaintenanceDate(schedule.getStartDate())
                    .maintenanceScheduleId(schedule.getMaintenanceScheduleId());
        }

        if (requiredUsageCount != null) {
            builder.requiredUsageCount(requiredUsageCount);
        }

        if (conflictBooking != null) {
            builder.conflictBookingId(conflictBooking.getBookingId())
                    .conflictOrderId(conflictBooking.getRentalOrder() != null
                            ? conflictBooking.getRentalOrder().getOrderId()
                            : null)
                    .conflictRentalStartDate(conflictBooking.getStartTime() != null
                            ? conflictBooking.getStartTime().toLocalDate()
                            : null)
                    .conflictRentalEndDate(conflictBooking.getEndTime() != null
                            ? conflictBooking.getEndTime().toLocalDate()
                            : conflictBooking.getStartTime() != null
                            ? conflictBooking.getStartTime().toLocalDate()
                            : null)
                    .conflictCustomerName(resolveCustomerName(conflictBooking));
        }

        return builder.build();
    }

    private boolean hasRentalMaintenanceConflict(MaintenanceSchedule schedule, BookingCalendar booking) {
        if (schedule == null || booking == null || schedule.getStartDate() == null || booking.getStartTime() == null) {
            return false;
        }
        LocalDate scheduleStart = schedule.getStartDate();
        LocalDate scheduleEnd = schedule.getEndDate() != null ? schedule.getEndDate() : scheduleStart;
        LocalDate bookingStart = booking.getStartTime().toLocalDate();
        LocalDate bookingEnd = booking.getEndTime() != null ? booking.getEndTime().toLocalDate() : bookingStart;

        return !scheduleEnd.isBefore(bookingStart) && !scheduleStart.isAfter(bookingEnd);
    }

    private String resolveCustomerName(BookingCalendar booking) {
        if (booking.getRentalOrder() != null && booking.getRentalOrder().getCustomer() != null) {
            return booking.getRentalOrder().getCustomer().getFullName();
        }
        return null;
    }
}


