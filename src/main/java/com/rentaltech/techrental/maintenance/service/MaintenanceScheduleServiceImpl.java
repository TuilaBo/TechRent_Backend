package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.model.MaintenanceScheduleStatus;
import com.rentaltech.techrental.maintenance.model.dto.MaintenanceScheduleByCategoryRequestDto;
import com.rentaltech.techrental.maintenance.model.dto.MaintenanceScheduleByUsageRequestDto;
import com.rentaltech.techrental.maintenance.model.dto.PriorityMaintenanceDeviceDto;
import com.rentaltech.techrental.maintenance.repository.MaintenanceScheduleCustomRepository;
import com.rentaltech.techrental.maintenance.repository.MaintenanceScheduleRepository;
import com.rentaltech.techrental.rentalorder.model.BookingCalendar;
import com.rentaltech.techrental.rentalorder.model.BookingStatus;
import com.rentaltech.techrental.rentalorder.repository.BookingCalendarRepository;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceScheduleServiceImpl implements MaintenanceScheduleService {

    private final MaintenanceScheduleRepository scheduleRepository;
    private final MaintenanceScheduleCustomRepository scheduleCustomRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCategoryRepository deviceCategoryRepository;
    private final BookingCalendarRepository bookingCalendarRepository;
    private final ImageStorageService imageStorageService;

    private static final long RENTAL_CONFLICT_LOOKAHEAD_DAYS = 14L;

    @Override
    @Transactional
    public MaintenanceSchedule createSchedule(Long deviceId, LocalDate startDate, LocalDate endDate, MaintenanceScheduleStatus status) {
        // Validate device tồn tại
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị với ID: " + deviceId));

        // Validate endDate >= startDate
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu");
        }

        // Không cho phép một thiết bị có 2 lịch bảo trì trùng khoảng thời gian
        assertNoScheduleConflict(device.getDeviceId(), startDate, endDate, null);

        // Mặc định STARTED nếu không truyền status
        MaintenanceScheduleStatus scheduleStatus = status != null ? status : MaintenanceScheduleStatus.STARTED;
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .device(device)
                .startDate(startDate)
                .endDate(endDate)
                .status(scheduleStatus.name())
                .createdAt(LocalDateTime.now())
                .build();
        MaintenanceSchedule saved = scheduleRepository.save(schedule);

        // Update device status nếu bảo trì bắt đầu từ hôm nay trở về trước
        if (startDate != null && (startDate.isBefore(LocalDate.now()) || startDate.isEqual(LocalDate.now()))) {
            if (scheduleStatus == MaintenanceScheduleStatus.STARTED) {
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
    public MaintenanceSchedule updateStatus(Long maintenanceScheduleId, MaintenanceScheduleStatus status, List<String> evidenceUrls, String username) {
        MaintenanceSchedule schedule = scheduleRepository.findById(maintenanceScheduleId).orElseThrow();
        return applyStatusAndEvidence(schedule, status, evidenceUrls, false, username);
    }

    @Override
    @Transactional
    public MaintenanceSchedule updateStatusWithUploads(Long maintenanceScheduleId,
                                                       MaintenanceScheduleStatus status,
                                                       List<String> evidenceUrls,
                                                       List<MultipartFile> files,
                                                       String username) {
        MaintenanceSchedule schedule = scheduleRepository.findById(maintenanceScheduleId).orElseThrow();
        List<String> combined = new ArrayList<>();
        if (evidenceUrls != null) {
            combined.addAll(evidenceUrls);
        }
        if (!CollectionUtils.isEmpty(files)) {
            int sequence = 1;
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String label = String.format("schedule-%d-evidence-%d", maintenanceScheduleId, sequence++);
                String url = imageStorageService.uploadMaintenanceEvidence(file, maintenanceScheduleId, label);
                combined.add(url);
            }
        }
        List<String> finalEvidence = combined.isEmpty() ? evidenceUrls : combined;
        return applyStatusAndEvidence(schedule, status, finalEvidence, true, username);
    }

    @Override
    @Transactional
    public List<MaintenanceSchedule> createSchedulesByCategory(MaintenanceScheduleByCategoryRequestDto request) {
        DeviceCategory category = deviceCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục thiết bị: " + request.getCategoryId()));

        // Lấy đúng danh sách device theo category bằng JPA, không quét toàn bộ
        List<Device> devices = deviceRepository.findByDeviceModel_DeviceCategory_DeviceCategoryId(category.getDeviceCategoryId());

        if (devices.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thiết bị thuộc danh mục: " + category.getDeviceCategoryName());
        }

        LocalDate endDate = request.getEndDate();
        if (request.getDurationDays() != null && request.getDurationDays() > 0) {
            endDate = request.getStartDate().plusDays(request.getDurationDays() - 1);
        }

        // Validate endDate >= startDate
        if (endDate != null && request.getStartDate() != null && endDate.isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu");
        }

        List<MaintenanceSchedule> schedules = new ArrayList<>();
        LocalDate today = LocalDate.now();
        MaintenanceScheduleStatus scheduleStatus = request.getStatus() != null ? request.getStatus() : MaintenanceScheduleStatus.STARTED;
        
        for (Device device : devices) {
            // Validate device tồn tại (đã có trong list, nhưng double check)
            if (device == null || device.getDeviceId() == null) {
                continue;
            }

            // validate conflict cho từng thiết bị trong category
            assertNoScheduleConflict(device.getDeviceId(), request.getStartDate(), endDate, null);

            MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                    .device(device)
                    .startDate(request.getStartDate())
                    .endDate(endDate)
                    .status(scheduleStatus.name())
                    .createdAt(LocalDateTime.now())
                    .build();
            MaintenanceSchedule saved = scheduleRepository.save(schedule);
            schedules.add(saved);

            // Update device status if maintenance starts today or earlier
            if (request.getStartDate() != null && 
                (request.getStartDate().isBefore(today) || request.getStartDate().isEqual(today))) {
                if (scheduleStatus == MaintenanceScheduleStatus.STARTED) {
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
        if (request.getUsageDays() == null || request.getUsageDays() <= 0) {
            throw new IllegalArgumentException("usageDays phải lớn hơn 0");
        }

        LocalDate endDate = request.getEndDate();
        if (request.getDurationDays() != null && request.getDurationDays() > 0) {
            endDate = request.getStartDate().plusDays(request.getDurationDays() - 1);
        }

        // Validate endDate >= startDate
        if (endDate != null && request.getStartDate() != null && endDate.isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu");
        }

        EnumSet<BookingStatus> countedStatuses = EnumSet.of(BookingStatus.COMPLETED);
        Map<Long, Long> usageDaysByDevice = bookingCalendarRepository.findAll().stream()
                .filter(booking -> booking.getDevice() != null
                        && booking.getDevice().getDeviceId() != null
                        && booking.getStartTime() != null
                        && booking.getEndTime() != null
                        && countedStatuses.contains(booking.getStatus()))
                .collect(Collectors.groupingBy(
                        booking -> booking.getDevice().getDeviceId(),
                        Collectors.summingLong(booking -> {
                            long days = ChronoUnit.DAYS.between(
                                    booking.getStartTime().toLocalDate(),
                                    booking.getEndTime().toLocalDate());
                            return Math.max(days, 1L);
                        })
                ));

        List<Long> deviceIds = usageDaysByDevice.entrySet().stream()
                .filter(entry -> entry.getValue() >= request.getUsageDays())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (deviceIds.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thiết bị nào có tổng số ngày thuê >= " + request.getUsageDays());
        }

        List<Device> devices = deviceRepository.findAllById(deviceIds);
        
        // Validate devices tồn tại
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thiết bị với các ID đã lọc");
        }

        List<MaintenanceSchedule> schedules = new ArrayList<>();
        for (Device device : devices) {
            // Validate device tồn tại
            if (device == null || device.getDeviceId() == null) {
                continue;
            }

            MaintenanceScheduleStatus scheduleStatus =
                    request.getStatus() != null ? request.getStatus() : MaintenanceScheduleStatus.STARTED;

            // validate conflict cho từng thiết bị theo usage
            assertNoScheduleConflict(device.getDeviceId(), request.getStartDate(), endDate, null);

            MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                    .device(device)
                    .startDate(request.getStartDate())
                    .endDate(endDate)
                    .status(scheduleStatus.name())
                    .createdAt(LocalDateTime.now())
                    .build();
            schedules.add(scheduleRepository.save(schedule));
        }

        return schedules;
    }

    @Override
    @Transactional
    public List<PriorityMaintenanceDeviceDto> getPriorityMaintenanceDevices() {
        List<PriorityMaintenanceDeviceDto> priorityDevices = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lookAhead = now.plusDays(RENTAL_CONFLICT_LOOKAHEAD_DAYS);
        LocalDate today = LocalDate.now();
        
        // Query upcoming bookings để lấy device IDs
        List<BookingCalendar> upcomingBookings = bookingCalendarRepository.findUpcomingBookings(
                now,
                lookAhead,
                EnumSet.of(BookingStatus.BOOKED, BookingStatus.ACTIVE)
        );
        Map<Long, List<BookingCalendar>> bookingsByDevice = upcomingBookings.stream()
                .filter(booking -> booking.getDevice() != null && booking.getDevice().getDeviceId() != null)
                .collect(Collectors.groupingBy(booking -> booking.getDevice().getDeviceId()));

        // Query upcoming schedules để lấy device IDs
        List<MaintenanceSchedule> upcomingSchedules = scheduleRepository.findByStartDateGreaterThanEqual(today);
        Map<Long, List<MaintenanceSchedule>> schedulesByDevice = upcomingSchedules.stream()
                .filter(schedule -> schedule.getDevice() != null && schedule.getDevice().getDeviceId() != null)
                .collect(Collectors.groupingBy(schedule -> schedule.getDevice().getDeviceId()));

        // Collect device IDs từ các nguồn
        Set<Long> relevantDeviceIds = new HashSet<>();
        relevantDeviceIds.addAll(bookingsByDevice.keySet());
        relevantDeviceIds.addAll(schedulesByDevice.keySet());

        // Load chỉ những devices cần thiết
        List<Device> devices = relevantDeviceIds.isEmpty() 
                ? List.of() 
                : deviceRepository.findAllById(relevantDeviceIds);

        for (Device device : devices) {
            List<MaintenanceSchedule> deviceSchedules = schedulesByDevice.getOrDefault(device.getDeviceId(), List.of()).stream()
                    .sorted(Comparator.comparing(MaintenanceSchedule::getStartDate, Comparator.nullsLast(LocalDate::compareTo)))
                    .collect(Collectors.toList());
            MaintenanceSchedule nextSchedule = deviceSchedules.isEmpty() ? null : deviceSchedules.get(0);
            
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

    @Override
    public List<MaintenanceSchedule> getInactiveMaintenanceSchedules() {
        return scheduleCustomRepository.findInactiveMaintenanceSchedules();
    }

    @Override
    @Transactional
    public Page<MaintenanceSchedule> listByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate và endDate không được để trống");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate phải lớn hơn hoặc bằng startDate");
        }
        return scheduleRepository.findByStartDateBetween(startDate, endDate, pageable);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long maintenanceScheduleId) {
        MaintenanceSchedule schedule = scheduleRepository.findById(maintenanceScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch bảo trì với ID: " + maintenanceScheduleId));
        
        Device device = schedule.getDevice();
        MaintenanceScheduleStatus currentStatus = parseStatus(schedule.getStatus());
        
        // Nếu schedule đang STARTED và device đang UNDER_MAINTENANCE,
        // kiểm tra xem có schedule active khác không, nếu không thì set device về AVAILABLE
        if (device != null && currentStatus != null 
                && currentStatus == MaintenanceScheduleStatus.STARTED) {
            if (device.getStatus() == DeviceStatus.UNDER_MAINTENANCE) {
                // Kiểm tra xem có schedule active khác cho device này không
                List<MaintenanceSchedule> activeSchedules = scheduleRepository.findByDevice_DeviceId(device.getDeviceId())
                        .stream()
                        .filter(s -> s.getMaintenanceScheduleId() != maintenanceScheduleId)
                        .filter(s -> {
                            MaintenanceScheduleStatus sStatus = parseStatus(s.getStatus());
                            return sStatus != null && sStatus == MaintenanceScheduleStatus.STARTED;
                        })
                        .collect(Collectors.toList());
                
                // Nếu không còn schedule active nào, set device về AVAILABLE
                if (activeSchedules.isEmpty()) {
                    device.setStatus(DeviceStatus.AVAILABLE);
                    deviceRepository.save(device);
                }
            }
        }
        
        scheduleRepository.deleteById(maintenanceScheduleId);
    }

    @Override
    @Transactional
    public MaintenanceSchedule getSchedule(Long maintenanceScheduleId) {
        return scheduleRepository.findById(maintenanceScheduleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy lịch bảo trì với ID: " + maintenanceScheduleId));
    }

    private MaintenanceSchedule applyStatusAndEvidence(MaintenanceSchedule schedule,
                                                       MaintenanceScheduleStatus status,
                                                       List<String> evidenceUrls,
                                                       boolean appendEvidence,
                                                       String username) {
        Device device = schedule.getDevice();
        MaintenanceScheduleStatus effectiveStatus = status != null ? status : parseStatus(schedule.getStatus());
        if (status != null) {
            schedule.setStatus(status.name());
        }
        if (evidenceUrls != null) {
            List<String> sanitized = evidenceUrls.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toList());
            List<String> target = schedule.getEvidenceUrls();
            if (target == null) {
                target = new ArrayList<>();
                schedule.setEvidenceUrls(target);
            }
            if (!appendEvidence) {
                target.clear();
            }
            target.addAll(sanitized);
        }
        // Lưu thông tin người cập nhật
        schedule.setUpdatedBy(username);
        schedule.setUpdatedAt(LocalDateTime.now());
        MaintenanceSchedule saved = scheduleRepository.save(schedule);

        if (device != null && effectiveStatus != null) {
            if (effectiveStatus == MaintenanceScheduleStatus.STARTED) {
                // Đang bảo trì -> thiết bị chuyển sang UNDER_MAINTENANCE
                device.setStatus(DeviceStatus.UNDER_MAINTENANCE);
                deviceRepository.save(device);
            } else if (effectiveStatus == MaintenanceScheduleStatus.COMPLETED) {
                // Bảo trì hoàn thành -> thiết bị sẵn sàng cho thuê lại
                device.setStatus(DeviceStatus.AVAILABLE);
                deviceRepository.save(device);
            } else if (effectiveStatus == MaintenanceScheduleStatus.FAILED) {
                // Bảo trì thất bại -> thiết bị hỏng, không còn dùng cho thuê
                device.setStatus(DeviceStatus.DAMAGED);
                deviceRepository.save(device);
            }
        }

        return saved;
    }

    /**
     * Không cho phép một thiết bị có nhiều hơn 1 lịch bảo trì trùng khoảng thời gian.
     *
     * @param deviceId  thiết bị cần kiểm tra
     * @param startDate ngày bắt đầu lịch mới
     * @param endDate   ngày kết thúc lịch mới (có thể null -> dùng startDate)
     * @param excludeId bỏ qua scheduleId này (dùng khi update nếu sau này cần)
     */
    private void assertNoScheduleConflict(Long deviceId,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          Long excludeId) {
        if (deviceId == null || startDate == null) {
            return;
        }
        LocalDate newStart = startDate;
        LocalDate newEnd = (endDate != null ? endDate : startDate);

        List<MaintenanceSchedule> existing = scheduleRepository.findByDevice_DeviceId(deviceId);
        boolean hasConflict = existing.stream()
                .filter(s -> excludeId == null || !excludeId.equals(s.getMaintenanceScheduleId()))
                .anyMatch(s -> {
                    LocalDate sStart = s.getStartDate();
                    if (sStart == null) {
                        return false;
                    }
                    LocalDate sEnd = (s.getEndDate() != null ? s.getEndDate() : sStart);
                    // overlap nếu !(existingEnd < newStart || existingStart > newEnd)
                    return !sEnd.isBefore(newStart) && !sStart.isAfter(newEnd);
                });

        if (hasConflict) {
            throw new IllegalArgumentException(
                    "Thiết bị ID=" + deviceId + " đã có lịch bảo trì trong khoảng " + newStart + " đến " + newEnd);
        }
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
                    .nextMaintenanceEndDate(schedule.getEndDate())
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

    private MaintenanceScheduleStatus parseStatus(String statusValue) {
        if (!StringUtils.hasText(statusValue)) {
            return null;
        }
        try {
            return MaintenanceScheduleStatus.valueOf(statusValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
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


